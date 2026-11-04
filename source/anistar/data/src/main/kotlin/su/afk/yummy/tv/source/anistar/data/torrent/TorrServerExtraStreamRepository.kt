package su.afk.yummy.tv.source.anistar.data.torrent

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import java.net.URI
import javax.inject.Inject

/**
 * Resolves an internal TorrServer reference into a normal HTTP stream understood by Media3.
 * The reference contains only a torrent hash and file index; Basic authentication is returned
 * as stream headers when configured on the server client.
 */
class TorrServerExtraStreamRepository @Inject constructor(
    private val torrServer: TorrServerClient,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = url.startsWith(SOURCE_PREFIX, ignoreCase = true)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val reference = parseReference(url)
            ?: return PlayerStreamResolveResult.Failed("Invalid TorrServer source reference")
        return runCatching {
            PlayerStreamResolveResult.Stream(
                url = torrServer.playbackUrl(reference.hash, reference.fileIndex),
                headers = torrServer.playbackHeaders(),
            )
        }.getOrElse {
            PlayerStreamResolveResult.Failed("Unable to create TorrServer playback URL")
        }
    }

    private fun parseReference(value: String): TorrServerReference? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (!uri.scheme.equals("source", ignoreCase = true) ||
            !uri.host.equals("torrserver", ignoreCase = true) ||
            uri.rawQuery != null || uri.fragment != null
        ) return null
        val parts = uri.path.trim('/').split('/')
        if (parts.size != 2) return null
        val hash = parts[0].takeIf { it.matches(HASH_PATTERN) } ?: return null
        val index = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return TorrServerReference(hash, index)
    }

    private data class TorrServerReference(val hash: String, val fileIndex: Int)

    private companion object {
        const val SOURCE_PREFIX = "source://torrserver/"
        val HASH_PATTERN = Regex("[A-Fa-f0-9]{20,64}")
    }
}
