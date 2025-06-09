package sf.sfis.miniesb.utility;

import java.util.Optional;
import java.util.function.Function;

public class GetterAccess {
	public static <T, R> Optional<R> get(T root, Function<T, R> mapper) {
		return Optional.ofNullable(root).map(mapper);
	}

	public static <T, R1, R2> Optional<R2> get(T root, Function<T, R1> f1, Function<R1, R2> f2) {
		return Optional.ofNullable(root).map(f1).map(f2);
	}

	public static <T, R1, R2, R3> Optional<R3> get(T root, Function<T, R1> f1, Function<R1, R2> f2, Function<R2, R3> f3) {
		return Optional.ofNullable(root).map(f1).map(f2).map(f3);
	}

	public static <T, R1, R2, R3, R4> Optional<R4> get(T root, Function<T, R1> f1, Function<R1, R2> f2, Function<R2, R3> f3,
			Function<R3, R4> f4) {
		return Optional.ofNullable(root).map(f1).map(f2).map(f3).map(f4);
	}
}
