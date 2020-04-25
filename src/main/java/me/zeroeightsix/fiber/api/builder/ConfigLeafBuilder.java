package me.zeroeightsix.fiber.api.builder;

import me.zeroeightsix.fiber.api.FiberId;
import me.zeroeightsix.fiber.api.exception.RuntimeFiberException;
import me.zeroeightsix.fiber.api.schema.type.ConfigType;
import me.zeroeightsix.fiber.api.schema.type.derived.DerivedType;
import me.zeroeightsix.fiber.api.tree.ConfigAttribute;
import me.zeroeightsix.fiber.api.tree.ConfigLeaf;
import me.zeroeightsix.fiber.api.tree.ConfigNode;
import me.zeroeightsix.fiber.api.tree.ConfigTree;
import me.zeroeightsix.fiber.impl.builder.ConfigNodeBuilder;
import me.zeroeightsix.fiber.impl.tree.ConfigLeafImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A builder for {@code ConfigLeaf}s.
 *
 * @param <T> the type of value the produced {@code ConfigLeaf} will hold
 * @see ConfigLeaf
 */
public class ConfigLeafBuilder<T, R> extends ConfigNodeBuilder {

    public static <T, R> ConfigLeafBuilder<T, R> create(ConfigTreeBuilder parentNode, @Nonnull String name, @Nonnull DerivedType<R, T, ?> type) {
        return new ConfigLeafBuilder<>(parentNode, name, type.getSerializedType(), type::toRuntimeType, type::toSerializedType);
    }

    public static <T> ConfigLeafBuilder<T, T> create(ConfigTreeBuilder parentNode, @Nonnull String name, @Nonnull ConfigType<T> type) {
        return new ConfigLeafBuilder<>(parentNode, name, type, Function.identity(), Function.identity());
    }

    @Nonnull
    protected final ConfigType<T> type;
    protected final Function<T, R> f;
    protected final Function<R, T> f0;

    @Nullable
    private T defaultValue = null;

    private BiConsumer<T, T> consumer = (t, t2) -> { };

    /**
     * Creates a new scalar {@code ConfigLeafBuilder}.
     * @param parentNode the {@code ConfigTreeBuilder} this builder originates from
     * @param name the name of the {@code ConfigLeaf} produced by this builder
     * @param type       the class object representing the type of values this builder will create settings for
     * @param f
     * @param f0
     */
    private ConfigLeafBuilder(ConfigTreeBuilder parentNode, @Nonnull String name, @Nonnull ConfigType<T> type, Function<T, R> f, Function<R, T> f0) {
        super(parentNode, name);
        this.type = type;
        this.f = f;
        this.f0 = f0;
    }

    @Nonnull
    public ConfigType<T> getType() {
        return type;
    }

    /**
     * Sets the {@code ConfigLeaf}'s name.
     *
     * @param name the name
     * @return {@code this} builder
     * @see ConfigTree#lookupLeaf
     */
    @Override
    public ConfigLeafBuilder<T, R> withName(@Nonnull String name) {
        super.withName(name);
        return this;
    }

    /**
     * Sets the {@code ConfigLeaf}'s comment.
     *
     * <p> If {@code null}, or if this method is never called, the {@code ConfigLeaf} won't have a comment.
     * An empty comment (non null, but only consisting of whitespace) will be serialised.
     *
     * @param comment the comment
     * @return {@code this} builder
     */
    @Override
    public ConfigLeafBuilder<T, R> withComment(String comment) {
        super.withComment(comment);
        return this;
    }

    /**
     * Adds a {@link ConfigAttribute} to the built {@code ConfigLeaf}.
     *
     * @param id           the id of the attribute
     * @param type         the class object representing the type of values stored in the attribute
     * @param defaultValue the attribute's default value
     * @param <A>          the type of values stored in the attribute
     * @return {@code this}, for chaining
     * @see ConfigNode#getAttributes()
     */
    @Override
    public <A> ConfigLeafBuilder<T, R> withAttribute(FiberId id, ConfigType<A> type, A defaultValue) {
        super.withAttribute(id, type, defaultValue);
        return this;
    }

    /**
     * Adds a listener to the {@code ConfigLeaf}.
     *
     * <p> Listeners are called when the value of a {@code ConfigLeaf} is changed. They are of type {@link BiConsumer}: the first argument being the old value, and the second argument being the new value.
     *
     * <p> Listeners set with this method are chained: if there was already one specified, a new listener is created that calls the old one first, and then the new one.
     *
     * @param consumer the listener
     * @return {@code this} builder
     */
    public ConfigLeafBuilder<T, R> withListener(BiConsumer<R, R> consumer) {
        // The newest consumer is called last -> listeners are called in the order they are added
        this.consumer = this.consumer.andThen((t, t2) -> consumer.accept(t == null ? null : this.f.apply(t), t2 == null ? null : this.f.apply(t2)));
        return this;
    }

    /**
     * Sets the default value.
     *
     * <p> If {@code null}, or if this method is never called, the {@code ConfigLeaf} will have no default value.
     *
     * <p> Note that every {@code ConfigLeaf} created from this builder will share a reference
     * to the given {@code defaultValue}. Immutability is encouraged.
     *
     * @param defaultValue the default value
     * @return {@code this} builder
     */
    public ConfigLeafBuilder<T, R> withDefaultValue(R defaultValue) {
        this.defaultValue = this.f0.apply(defaultValue);
        return this;
    }

    /**
     * Builds the {@code ConfigLeaf}.
     *
     * <p> If a parent was specified in the constructor, the {@code ConfigLeaf} will also be registered to its parent node.
     *
     * <p> This method should not be called multiple times <em>if the default value is intended to be mutated</em>.
     * Multiple calls will result in duplicated references to the default value.
     *
     * @return the {@code ConfigLeaf}
     */
    @Override
    public ConfigLeaf<T> build() {
        if (this.defaultValue != null) {
            if (!this.type.accepts(this.defaultValue)) {
                throw new RuntimeFiberException("Default value '" + this.defaultValue + "' does not satisfy constraints on type " + this.type);
            }
        }
        ConfigLeaf<T> built = new ConfigLeafImpl<>(Objects.requireNonNull(name, "Cannot build a value without a name"), type, comment, defaultValue, consumer);
        built.getAttributes().putAll(this.attributes);

        if (parent != null) {
            // We don't know what kind of evil collection we're about to add a node to.
            // Though, we don't really want to throw an exception on this method because no developer likes try-catching every setting they build.
            // Let's tread with caution.
            try {
                parent.getItems().add(built);
            } catch (RuntimeFiberException e) {
                throw new RuntimeFiberException("Failed to register leaf to node", e);
            }
        }

        return built;
    }

    public ConfigTreeBuilder finishValue() {
        return finishValue(n -> {});
    }

    public ConfigTreeBuilder finishValue(Consumer<ConfigLeaf<T>> action) {
        if (parent instanceof ConfigTreeBuilder) {
            action.accept(build());
            return (ConfigTreeBuilder) parent;
        } else {
            throw new IllegalStateException("finishValue should not be called for an independent builder. Use build instead.");
        }
    }
}
