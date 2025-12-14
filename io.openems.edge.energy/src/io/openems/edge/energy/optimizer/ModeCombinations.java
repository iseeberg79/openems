package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * Handles combinations of {@link EshWithDifferentModes} Modes.
 * 
 * <p>
 * First {@link ModeCombination} (get via {@link #getDefault()}) is the default
 * of of all ESHs.
 */
public record ModeCombinations(ImmutableList<ModeCombination> combinations,
		ImmutableMap<IntArrayKey, ModeCombination> indexLookup) {

	/**
	 * Wrapper for int[] to use as HashMap key with proper hashCode/equals.
	 */
	public static final class IntArrayKey {
		private final int[] array;
		private final int hashCode;

		public IntArrayKey(int[] array) {
			this.array = array;
			this.hashCode = Arrays.hashCode(array);
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof IntArrayKey other) {
				return Arrays.equals(this.array, other.array);
			}
			return false;
		}
	}

	public static final ImmutableList<List<String>> INFEASIBLE_COMBINATIONS = ImmutableList.<List<String>>builder() //
			.add(List.of("Evse.Controller.Single:SURPLUS", "Controller.Ess.Time-Of-Use-Tariff:DELAY_DISCHARGE")) //
			.add(List.of("Evse.Controller.Single:SURPLUS", "Controller.Ess.Time-Of-Use-Tariff:CHARGE_GRID")) //
			.build();

	/**
	 * Holds one Mode.
	 */
	public static record Mode(EnergyScheduleHandler.WithDifferentModes esh, int index, String name) {

		protected static Mode from(EnergyScheduleHandler.WithDifferentModes esh, int index) {
			final var b = new StringBuilder();
			final var factoryPid = esh.getParentFactoryPid();
			if (!factoryPid.isBlank()) {
				b.append(factoryPid).append(":");
			}
			final var name = b.append(esh.toModeString(index)).toString();
			return new Mode(esh, index, name);
		}

		@Override
		public final String toString() {
			return this.name();
		}
	}

	public static record ModeCombination(//
			int index, //
			// mode(0) = eshsWithDifferentModes[0],
			// mode(1) = eshsWithDifferentModes[1], etc.
			ImmutableList<Mode> modes, //
			// Pre-computed mode indexes for fast lookup
			int[] modeIndexes) {

		/**
		 * Creates a ModeCombination with pre-computed modeIndexes.
		 *
		 * @param index the combination index
		 * @param modes the list of modes
		 * @return new ModeCombination
		 */
		public static ModeCombination of(int index, ImmutableList<Mode> modes) {
			var indexes = modes.stream().mapToInt(Mode::index).toArray();
			return new ModeCombination(index, modes, indexes);
		}

		/**
		 * Gets the {@link Mode} for {@link EnergyScheduleHandler} at index i.
		 *
		 * @param i index
		 * @return the {@link Mode}
		 */
		public final Mode mode(int i) {
			return this.modes.get(i);
		}
	}

	protected static class Builder {

		private final List<List<String>> infeasible = new ArrayList<List<String>>();
		private final List<ModeCombination> combinations = new ArrayList<ModeCombination>();

		private int nextIndex = 0;

		public Builder addInfeasibles(List<List<String>> infeasibleCombinations) {
			this.infeasible.addAll(infeasibleCombinations);
			return this;
		}

		public Builder addInfeasible(String... infeasibleCombination) {
			this.infeasible.add(Arrays.asList(infeasibleCombination));
			return this;
		}

		public synchronized Builder addCombination(List<Mode> modes) {
			if (this.infeasible.stream() // Is Combination marked as infeasible?
					.anyMatch(ifc -> ifc.stream() //
							.allMatch(c -> modes.stream() //
									.anyMatch(m -> c.equals(m.name))))) {
				return this;
			}
			if (this.combinations.stream() // Is Combination already existing?
					.anyMatch(c -> c.modes.containsAll(modes))) {
				return this;
			}

			var combination = ModeCombination.of(this.nextIndex++, ImmutableList.copyOf(modes));
			this.combinations.add(combination);
			return this;
		}

		public ModeCombinations build() {
			var combinationsList = ImmutableList.copyOf(this.combinations);
			// Build lookup map for O(1) access by mode indexes
			var lookupBuilder = ImmutableMap.<IntArrayKey, ModeCombination>builder();
			for (var c : combinationsList) {
				lookupBuilder.put(new IntArrayKey(c.modeIndexes()), c);
			}
			return new ModeCombinations(combinationsList, lookupBuilder.build());
		}
	}

	/**
	 * Builds all combinations of Modes; excluding the provided
	 * infeasibleCombinations.
	 * 
	 * @param eshs                   the list of {@link EnergyScheduleHandler}s
	 * @param infeasibleCombinations a list of infeasible combinations. String is in
	 *                               the format {@link Mode#name()}.
	 * @return list of {@link ModeCombination}s
	 */
	public static ModeCombinations fromGlobalOptimizationContext(GlobalOptimizationContext goc) {
		final var result = new ModeCombinations.Builder() //
				.addInfeasibles(INFEASIBLE_COMBINATIONS);

		// Set first ModeCombination as default (index = 0) Mode for all ESHs.
		result.addCombination(goc.eshsWithDifferentModes().stream() //
				.map(esh -> Mode.from(esh, 0)) //
				.toList());

		var cp = Lists.cartesianProduct(//
				goc.eshsWithDifferentModes().stream() //
						.map(esh -> IntStream.range(0, esh.getNumberOfAvailableModes()) //
								.mapToObj(i -> Mode.from(esh, i)) //
								.collect(toImmutableList())) //
						.collect(toImmutableList())); //
		cp.forEach(mss -> result.addCombination(mss));
		return result.build();
	}

	/**
	 * Gets the default {@link ModeCombinations} at index 0.
	 * 
	 * @return the {@link ModeCombinations} or null if list is empty
	 */
	public ModeCombination getDefault() {
		if (this.combinations.isEmpty()) {
			return null;
		}
		return this.combinations.get(0);
	}

	/**
	 * Gets the {@link ModeCombinations} at given index.
	 * 
	 * @param index the index
	 * @return the {@link ModeCombinations} or null if list is empty
	 */
	public ModeCombination get(int index) {
		if (this.combinations.isEmpty()) {
			return null;
		}
		return this.combinations.get(index);
	}

	/**
	 * Gets the number of possible {@link ModeCombination}s.
	 * 
	 * @return size
	 */
	public int size() {
		return this.combinations.size();
	}

	public boolean isEmpty() {
		return this.combinations.isEmpty();
	}

	/**
	 * Finds the matching 'old' {@link ModeCombination} in the current list of
	 * {@link ModeCombination}s.
	 * 
	 * @param previousModeCombination the 'old' {@link ModeCombination}
	 * @param modeCombinations        the current list of {@link ModeCombination}s
	 * @return matching {@link ModeCombination} or default
	 */
	public ModeCombination getMatchingOrDefault(ModeCombination previousModeCombination) {
		if (previousModeCombination == null) {
			return this.getDefault();
		}
		for (var thisMode : this.combinations) {
			if (previousModeCombination.modes().stream() //
					.allMatch(prev -> thisMode.modes().stream() //
							.anyMatch(m -> m.name().equals(prev.name())))) {
				// Found matching ModeCombination -> return
				return thisMode;
			}
		}
		return this.getDefault();
	}

	/**
	 * Gets the {@link ModeCombination} from the given {@link EnergyScheduleHandler}
	 * indexes.
	 *
	 * @param indexes the indexes
	 * @return the {@link ModeCombination}
	 */
	public ModeCombination getFromModeIndexesOrDefault(int[] indexes) {
		var result = this.indexLookup.get(new IntArrayKey(indexes));
		return result != null ? result : this.getDefault();
	}
}