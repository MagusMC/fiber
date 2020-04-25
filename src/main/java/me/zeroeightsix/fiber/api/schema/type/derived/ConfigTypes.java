package me.zeroeightsix.fiber.api.schema.type.derived;


import me.zeroeightsix.fiber.api.schema.type.*;
import me.zeroeightsix.fiber.impl.annotation.magic.TypeMagic;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConfigTypes {

    public static final BooleanDerivedType<Boolean> BOOLEAN =
            new BooleanDerivedType<>(Boolean.class, Function.identity(), Function.identity());

    /* Number-derived types */

    public static final NumberDerivedType<BigDecimal> UNBOUNDED_DECIMAL =
            makeNumber(BigDecimal.class, Function.identity(), Function.identity(), null, null, null);
    public static final NumberDerivedType<BigInteger> UNBOUNDED_INTEGER =
            makeNumber(BigInteger.class, BigDecimal::new, BigDecimal::toBigInteger, null, null, null);
    public static final NumberDerivedType<Byte> BYTE =
            makeNumber(Byte.class, BigDecimal::valueOf, BigDecimal::byteValue, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 1);
    public static final NumberDerivedType<Short> SHORT =
            makeNumber(Short.class, BigDecimal::valueOf, BigDecimal::shortValue, Short.MIN_VALUE, Short.MAX_VALUE, (short) 1);
    public static final NumberDerivedType<Integer> INTEGER =
            makeNumber(Integer.class, BigDecimal::valueOf, BigDecimal::intValue, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    public static final NumberDerivedType<Long> LONG =
            makeNumber(Long.class, BigDecimal::valueOf, BigDecimal::longValue, Long.MIN_VALUE, Long.MAX_VALUE, 1L);
    public static final NumberDerivedType<Float> FLOAT =
            makeNumber(Float.class, BigDecimal::valueOf, BigDecimal::floatValue, null, null, null);
    public static final NumberDerivedType<Double> DOUBLE =
            makeNumber(Double.class, BigDecimal::valueOf, BigDecimal::doubleValue, null, null, null);

    public static <N> NumberDerivedType<N> makeNumber(Class<N> actualType, Function<N, BigDecimal> f0, Function<BigDecimal, N> f, @Nullable N minValue, @Nullable N maxValue, @Nullable N precision) {
        return new NumberDerivedType<>(
                new DecimalConfigType(minValue == null ? null : f0.apply(minValue), maxValue == null ? null : f0.apply(maxValue), precision == null ? null : f0.apply(precision)),
                actualType,
                f,
                f0
        );
    }

    /* String-derived types */

    public static final StringDerivedType<String> STRING =
            new StringDerivedType<>(StringConfigType.DEFAULT_STRING, String.class, Function.identity(), Function.identity());

    public static final StringDerivedType<Character> CHARACTER =
            STRING.withMinLength(1).withMaxLength(1).derive(Character.class, s -> s.charAt(0), Object::toString);

    /* Enum-derived types */

    public static <E extends Enum<E>> EnumDerivedType<E> makeEnum(Class<E> enumType) {
        Set<String> validValues = Arrays.stream(enumType.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
        return new EnumDerivedType<>(new EnumConfigType(validValues), enumType, e -> Enum.valueOf(enumType, e), Enum::name);
    }

    /* List-derived types */

    public static <E0, E, U extends DerivedType<E, E0, ?>> ListDerivedType<List<E>, E0> makeList(U elementType) {
        return new ListDerivedType<>(
                ListConfigType.of(elementType.getSerializedType()), List.class,
                l -> {
                    List<E> ret = new ArrayList<>();
                    for (E0 e0 : l) {
                        ret.add(elementType.toRuntimeType(e0));
                    }
                    return Collections.unmodifiableList(ret);
                },
                l -> {
                    List<E0> ret = new ArrayList<>();
                    for (E e : l) {
                        ret.add(elementType.toSerializedType(e));
                    }
                    return Collections.unmodifiableList(ret);
                }
        );
    }

    public static <E0, E, U extends DerivedType<E, E0, ?>> ListDerivedType<Set<E>, E0> makeSet(U elementType) {
        return makeList(elementType).withUniqueElements().derive(Set.class, HashSet::new, ArrayList::new);
    }

    @SuppressWarnings("unchecked")
    public static <E0, E, U extends DerivedType<E, E0, ? extends ConfigType<E0>>> ListDerivedType<E[], E0> makeArray(U elementType) {
        E[] z = (E[]) Array.newInstance(elementType.getRuntimeType(), 0);
        Class<E[]> arrType = (Class<E[]>) z.getClass();
        // account for arrays of primitives - we must avoid autocasting to Object[]
        return (ListDerivedType<E[], E0>) (ListDerivedType<?, ?>) makeList((DerivedType<Object, Object, ?>) elementType).derive((Class<? super Object>) (Class<?>) arrType, l -> TypeMagic.unbox(l.toArray((Object[]) z)), a -> Arrays.asList((E[]) TypeMagic.box(a)));
    }

    /* Record-derived config types */

    public static <K, V, S> MapDerivedType<Map<K, V>, S> makeMap(StringDerivedType<K> keyConverter, DerivedType<V, S, ?> valueConverter) {
        return new MapDerivedType<>(
                new MapConfigType<>(valueConverter.getSerializedType(), 0, Integer.MAX_VALUE),
                Map.class,
                map -> {
                    Map<K, V> ret = new HashMap<>();
                    map.forEach((k, v) -> ret.put(keyConverter.toRuntimeType(k), valueConverter.toRuntimeType(v)));
                    return Collections.unmodifiableMap(ret);
                },
                map -> {
                    Map<String, S> ret = new HashMap<>();
                    map.forEach((k, v) -> ret.put(keyConverter.toSerializedType(k), valueConverter.toSerializedType(v)));
                    return ret;
                }
        );
    }
}
