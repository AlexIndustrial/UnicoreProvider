package ru.unicorecms.unicoreprovider.core

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import pro.gravit.launcher.base.request.auth.CheckServerRequest
import pro.gravit.launcher.base.request.auth.JoinServerRequest
//import pro.gravit.launcher.events.request.AuthRequestEvent
//import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent
//import pro.gravit.launcher.request.auth.AuthRequest
import pro.gravit.launchserver.LaunchServer
import pro.gravit.launchserver.auth.AuthException
import pro.gravit.launchserver.auth.core.AuthCoreProvider
//import pro.gravit.launchserver.auth.core.HttpAuthCoreProvider.*
import pro.gravit.launchserver.auth.core.User
import pro.gravit.launchserver.auth.core.UserSession
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportExit
import pro.gravit.launchserver.manangers.AuthManager
import pro.gravit.launchserver.socket.Client
import pro.gravit.launchserver.socket.response.auth.AuthResponse
import ru.unicorecms.unicoreprovider.core.dto.AuthReport
import ru.unicorecms.unicoreprovider.utils.UnicoreRequester
import java.io.IOException
import java.util.*

class UnicoreAuthProvider: AuthCoreProvider(), AuthSupportExit {
    var apiUrl: String? = null
    var apiKey: String? = null
    private lateinit var getUserByUsernameUrl: String
    private lateinit var getUserByUUIDUrl: String
    private lateinit var getUserByTokenUrl: String
    private lateinit var getAuthDetailsUrl: String
    private lateinit var refreshTokenUrl: String
    private lateinit var authorizeUrl: String
    private lateinit var deleteSessionUrl: String
    private lateinit var exitUserUrl: String
    private lateinit var checkServerUrl: String
    private lateinit var joinServerUrl: String

    @Transient
    private val logger: Logger = LogManager.getLogger()

    @Transient
    private lateinit var requester: UnicoreRequester

    override fun close() {}

    override fun getUserByUsername(username: String): User? {
        return try {
            requester.get(getUserByUsernameUrl, username).getOrThrow<HttpUser>()
        } catch (e: IOException) {
            null
        }
    }

    override fun getUserByUUID(uuid: UUID): User? {
        return try {
            requester.get(getUserByUUIDUrl, uuid.toString()).getOrThrow<HttpUser>()
        } catch (e: IOException) {
            null
        }
    }

    override fun getUserSessionByOAuthAccessToken(accessToken: String): UserSession? {
        return try {
            val result = requester.get(getUserByTokenUrl, bearer = accessToken)

            if (result.error != null) {
                if (result.error == AuthRequestEvent.OAUTH_TOKEN_EXPIRE)
                    throw OAuthAccessTokenExpired()

                return null
            }

            result.getOrThrow<HttpUserSession>()
        } catch (e: IOException) {
            null
        }
    }

    override fun refreshAccessToken(refreshToken: String, context: AuthResponse.AuthContext): AuthManager.AuthReport? {
        return try {
            val response = requester.post(refreshTokenUrl, RefreshTokenRequest(refreshToken, context)).getOrThrow<AuthReport>()

            return AuthManager.AuthReport(
                response.minecraftAccessToken,
                response.oauthAccessToken,
                response.oauthRefreshToken,
                response.oauthExpire * 1000,
                response.session
            )
        } catch (e: IOException) {
            null
        }
    }

    override fun getDetails(client: Client?): List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> {
        return try {
            requester.get(getAuthDetailsUrl).getOrThrow<GetAuthDetailsResponse>().details
        } catch (e: IOException) {
            logger.error(e)
            super.getDetails(client)
        }
    }

    override fun authorize(login: String, context: AuthResponse.AuthContext, password: AuthRequest.AuthPasswordInterface, minecraftAccess: Boolean): AuthManager.AuthReport {
        val result = requester.post(authorizeUrl, AuthorizeRequest(login, context, password, minecraftAccess))

        if (result.error != null)
            throw AuthException(result.error)

        val response = result.getOrThrow<AuthReport>()

        return AuthManager.AuthReport(
            response.minecraftAccessToken,
            response.oauthAccessToken,
            response.oauthRefreshToken,
            response.oauthExpire * 1000,
            response.session
        )
    }

    override fun deleteSession(session: UserSession): Boolean {
        return requester.post(deleteSessionUrl, session).response.isSuccessful
    }

    override fun exitUser(user: User): Boolean {
        return requester.post(exitUserUrl, user).response.isSuccessful
    }

    override fun checkServer(client: Client, username: String, serverID: String): User {
        return requester.post(checkServerUrl, CheckServerRequest(username, serverID)).getOrThrow<HttpUser>()
    }

    override fun joinServer(
        client: Client?,
        username: String?,
        uuid: UUID?,
        accessToken: String?,
        serverID: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun joinServer(client: Client, username: String, accessToken: String, serverID: String): Boolean {
        return requester.post(joinServerUrl, JoinServerRequest(username, accessToken, serverID)).response.isSuccessful
    }

    @Suppress("SENSELESS_COMPARISON")
    override fun init(server: LaunchServer) {
        // Init provider config
        if(apiUrl == null)
            throw IllegalArgumentException("UnicoreAuthProvider 'apiUrl' can't be null")

        if(apiKey == null)
            throw IllegalArgumentException("UnicoreAuthProvider 'apiKey' can't be null")

        // Set-up urls
        getUserByUsernameUrl = "$apiUrl/auth/gravit/getUserByUsername/%"
        getUserByUUIDUrl = "$apiUrl/auth/gravit/getUserByUUID/%"
        getUserByTokenUrl = "$apiUrl/auth/gravit/getUserByToken"
        getAuthDetailsUrl = "$apiUrl/auth/gravit/getAuthDetails"
        refreshTokenUrl = "$apiUrl/auth/gravit/refreshToken"
        authorizeUrl = "$apiUrl/auth/gravit/authorize"
        deleteSessionUrl = "$apiUrl/auth/gravit/deleteSession"
        exitUserUrl = "$apiUrl/auth/gravit/exitUser"
        checkServerUrl = "$apiUrl/auth/gravit/checkServer"
        joinServerUrl = "$apiUrl/auth/gravit/joinServer"

        // Build UnicoreRequester
        requester = UnicoreRequester(apiKey!!)
        logger.info("UnicoreAuthProvider successfully initialized")
    }
}
