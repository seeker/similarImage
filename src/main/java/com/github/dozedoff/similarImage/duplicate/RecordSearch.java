package com.github.dozedoff.similarImage.duplicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.everpeace.search.BKTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Builds the necessary data structures from the supplied data to allow queries
 * for records with hashes at a given hamming-distance.
 * 
 * @author Nicholas Wright
 *
 */
public class RecordSearch {
	private static final Logger logger = LoggerFactory.getLogger(RecordSearch.class);
	private Multimap<Long, ImageRecord> groups;
	private BKTree<Long> bkTree;

	public RecordSearch(Collection<ImageRecord> dbRecords) {
		logger.info("Building Record search from {} records...", dbRecords.size());

		groupRecords(dbRecords);
		buildBkTree();
	}

	private void groupRecords(Collection<ImageRecord> dbRecords) {
		Stopwatch swGroup = Stopwatch.createStarted();
		this.groups = DuplicateUtil.groupByHash(dbRecords);
		swGroup.stop();

		logger.info("Grouped records into {} groups in {}", numberOfHashes(), swGroup);
	}

	private void buildBkTree() {
		logger.info("Building BK-Tree from {} hashes", numberOfHashes());

		Stopwatch swBuildTree = Stopwatch.createStarted();
		bkTree = BKTree.build(groups.keySet(), new CompareHammingDistance());
		swBuildTree.stop();

		logger.info("Took {} to build BK-tree with {} hashes", swBuildTree, numberOfHashes());
	}

	private int numberOfHashes() {
		return groups.keySet().size();
	}

	/**
	 * Create a map that only contains groups with more than one image.
	 * 
	 * @return new map containing only multi-image groups.
	 */
	public Multimap<Long, ImageRecord> removeSingleImageGroups(Multimap<Long, ImageRecord> sourceGroups) {
		Multimap<Long, ImageRecord> multiImageGroups = MultimapBuilder.hashKeys().hashSetValues().build();

		for (Entry<Long, Collection<ImageRecord>> group : sourceGroups.asMap().entrySet()) {
			if (group.getValue().size() > 1) {
				multiImageGroups.putAll(group.getKey(), group.getValue());
			}
		}
		
		return multiImageGroups;
	}

	/**
	 * For backwards compatibility, migration.
	 * 
	 * @param pHash
	 * @param hammingDistance
	 * @return
	 */
	@Deprecated
	public Set<Bucket<Long, ImageRecord>> searchTreeLegacy(long pHash, long hammingDistance) {
		Set<Long> hashes = bkTree.searchWithin(pHash, (double) hammingDistance);
		Set<Bucket<Long, ImageRecord>> buckets = new HashSet<>();

		for (Long hash : hashes) {
			buckets.add(new Bucket<Long, ImageRecord>(hash, groups.get(hash)));
		}

		return buckets;
	}

	/**
	 * Return all groups with exact matches and more than one image per match.
	 * 
	 * @return distinct list of matches
	 */
	public List<Long> exactMatch() {
		Multimap<Long, ImageRecord> multiImage = removeSingleImageGroups(groups);
		return new ArrayList<>(multiImage.keySet());
	}

	@Deprecated
	public HashMap<Long, Set<Bucket<Long, ImageRecord>>> sortExactMatchLegacy() {
		List<Long> keys = exactMatch();

		HashMap<Long, Set<Bucket<Long, ImageRecord>>> exactMatches;

		logger.info("Checking {} groups for size greater than 1", numberOfHashes());

		exactMatches = longToLegacyBucket(keys);

		logger.info("Found {} groups with more than 1 image", numberOfHashes() - keys.size());

		return exactMatches;
	}

	private HashMap<Long, Set<Bucket<Long, ImageRecord>>> longToLegacyBucket(List<Long> hashes) {
		HashMap<Long, Set<Bucket<Long, ImageRecord>>> buckets = new HashMap<>();

		for (Long key : hashes) {
			Set<Bucket<Long, ImageRecord>> set = new HashSet<>();
			set.add(new Bucket<Long, ImageRecord>(key, groups.get(key)));
			buckets.put(key, set);
		}

		return buckets;
	}

	/**
	 * For every hash, find the matches within the distance and add them to the
	 * set. Only return matches with more than one image.
	 * 
	 * @param hammingDistance
	 *            search for hashes up to and including this distance
	 * @return a set of matches with more than one image
	 */
	public List<Long> distanceMatch(long hammingDistance) {
		Set<Long> keySet = removeSingleImageGroups(groups).keySet();
		Set<Long> resultSet = new HashSet<Long>();

		for (Long key : keySet) {
			resultSet.addAll(bkTree.searchWithin(key, (double) hammingDistance));
		}

		return new ArrayList<>(resultSet);
	}

	/**
	 * For backwards compatibility, migration.
	 */
	@Deprecated
	public HashMap<Long, Set<Bucket<Long, ImageRecord>>> sortHammingDistanceLegacy(long hammingDistance) {
		return longToLegacyBucket(distanceMatch(hammingDistance));
	}
}
