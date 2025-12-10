package fun.eqad.ponyrace.playerdata;

import com.google.gson.*;
import org.bukkit.*;
import java.lang.reflect.Type;

public class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", src.getWorld().getName());
        obj.addProperty("x", src.getX());
        obj.addProperty("y", src.getY());
        obj.addProperty("z", src.getZ());
        obj.addProperty("yaw", src.getYaw());
        obj.addProperty("pitch", src.getPitch());
        return obj;
    }

    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        World world = Bukkit.getWorld(obj.get("world").getAsString());
        if (world == null) return null;
        return new Location(
                world,
                obj.get("x").getAsDouble(),
                obj.get("y").getAsDouble(),
                obj.get("z").getAsDouble(),
                obj.get("yaw").getAsFloat(),
                obj.get("pitch").getAsFloat()
        );
    }
}