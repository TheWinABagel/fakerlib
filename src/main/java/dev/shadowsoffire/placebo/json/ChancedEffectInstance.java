package dev.shadowsoffire.placebo.json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Represents a potion with a chance to receive this potion.
 */
/**
 * Primer for creating a {@link MobEffectInstance} with a random application chance and random amplifier.
 * <p>
 * Duration is determined by the caller when creating the real MobEffectInstance.
 */
public record ChancedEffectInstance(float chance, MobEffect effect, StepFunction amplifier, boolean ambient, boolean visible) {

    public static Codec<ChancedEffectInstance> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    PlaceboCodecs.nullableField(Codec.floatRange(0, 1), "chance", 1F).forGetter(ChancedEffectInstance::chance),
                    BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(ChancedEffectInstance::effect),
                    PlaceboCodecs.nullableField(StepFunction.CODEC, "amplifier", StepFunction.constant(0)).forGetter(ChancedEffectInstance::amplifier),
                    PlaceboCodecs.nullableField(Codec.BOOL, "ambient", true).forGetter(ChancedEffectInstance::ambient),
                    PlaceboCodecs.nullableField(Codec.BOOL, "visible", false).forGetter(ChancedEffectInstance::visible))
            .apply(inst, ChancedEffectInstance::new));

    /**
     * Special codec that makes the created effect instance deterministic.
     */
    public static Codec<ChancedEffectInstance> CONSTANT_CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Codec.unit(1F).optionalFieldOf("chance", 1F).forGetter(a -> 1F),
                    BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(ChancedEffectInstance::effect),
                    PlaceboCodecs.nullableField(Codec.intRange(0, 255), "amplifier", 0).xmap(i -> StepFunction.constant(i), sf -> (int) sf.min()).forGetter(ChancedEffectInstance::amplifier),
                    PlaceboCodecs.nullableField(Codec.BOOL, "ambient", true).forGetter(ChancedEffectInstance::ambient),
                    PlaceboCodecs.nullableField(Codec.BOOL, "visible", false).forGetter(ChancedEffectInstance::visible))
            .apply(inst, ChancedEffectInstance::new));

    public MobEffectInstance create(RandomSource rand, int duration) {
        return new MobEffectInstance(this.effect, duration, this.amplifier.getInt(rand.nextFloat()), this.ambient, this.visible);
    }

    public MobEffectInstance createDeterministic(int duration) {
        return new MobEffectInstance(this.effect, duration, this.amplifier.getInt(0), this.ambient, this.visible);
    }

    @Deprecated(forRemoval = true)
    public static class Deserializer implements JsonDeserializer<ChancedEffectInstance> {

        @Override
        public ChancedEffectInstance deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            float chance = obj.get("chance").getAsFloat();
            String _effect = obj.get("effect").getAsString();
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(_effect));
            if (effect == null) throw new JsonParseException("Attempted to construct a ChancedEffectInstance with invalid effect: " + _effect);
            boolean ambient = obj.has("ambient") ? obj.get("ambient").getAsBoolean() : true;
            boolean visible = obj.has("visible") ? obj.get("visible").getAsBoolean() : false;
            if (obj.has("amplifier")) {
                JsonObject range = obj.get("amplifier").getAsJsonObject();
                int min = range.get("min").getAsInt();
                int max = range.get("max").getAsInt();
                StepFunction func = min == max ? StepFunction.constant(min) : new StepFunction(min, max - min, 1);
                return new ChancedEffectInstance(chance, effect, func, ambient, visible);
            }
            return new ChancedEffectInstance(chance, effect, StepFunction.constant(0), ambient, visible);
        }
    }
}