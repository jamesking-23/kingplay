package com.kingplay.data.library.scan

data class LibraryDiff(
    /** Ids to load in full and upsert: new, changed, or everything when a full scan was requested. */
    val toLoad: List<Long>,
    val toDelete: List<Long>,
)

/**
 * Compares what is stored with what the platform reports.
 *
 * Safety rule: if the platform reports no files at all while the library has some, deletions are skipped.
 * That pattern means a revoked permission, an unmounted card or a rebuilding index, not a user who deleted
 * every file, and wiping the library would also orphan playlists.
 */
fun diffFingerprints(
    existing: Map<Long, Fingerprint>,
    scanned: Map<Long, Fingerprint>,
    forceAll: Boolean = false,
): LibraryDiff {
    val toLoad = scanned.entries
        .asSequence()
        .filter { (id, fp) ->
            val old = existing[id]
            forceAll || old == null || old.modifiedMs != fp.modifiedMs || old.sizeBytes != fp.sizeBytes
        }
        .map { it.key }
        .sorted()
        .toList()
    val suspiciousWipe = scanned.isEmpty() && existing.isNotEmpty()
    val toDelete = if (suspiciousWipe) emptyList() else existing.keys.filter { it !in scanned }.sorted()
    return LibraryDiff(toLoad, toDelete)
}
