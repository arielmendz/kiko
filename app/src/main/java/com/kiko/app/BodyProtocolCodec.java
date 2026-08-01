package com.kiko.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

final class BodyProtocolCodec {
    static final int MAX_MESSAGE_BYTES = 512;
    static final int MIN_TIMEOUT_MS = 100;
    static final int MAX_TIMEOUT_MS = 10_000;

    private static final Pattern IDENTIFIER = Pattern.compile(
            "^[A-Za-z0-9._:-]{1,64}$"
    );
    private static final Pattern INTEGER = Pattern.compile("^-?(?:0|[1-9][0-9]*)$");
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    byte[] encodeCommand(BodyProtocolCommand command)
            throws BodyProtocolException {
        JsonObject root = new JsonObject();
        root.addProperty("protocolVersion", BodyCapabilities.PROTOCOL_VERSION);
        root.addProperty("commandId", command.getCommandId());
        root.addProperty("type", command.getType().name());

        JsonObject arguments = new JsonObject();
        if (command.getType() == BodyProtocolCommand.Type.MOVE_STEPS) {
            arguments.addProperty("count", command.getStepCount());
        } else if (command.getType() == BodyProtocolCommand.Type.DANCE) {
            arguments.addProperty("routineId", command.getRoutineId());
        }
        root.add("arguments", arguments);
        root.addProperty("timeoutMs", command.getTimeoutMs());

        byte[] payload = encode(root);
        decodeCommand(payload);
        return payload;
    }

    BodyProtocolCommand decodeCommand(byte[] payload)
            throws BodyProtocolException {
        JsonObject root = decodeObject(payload, "command_must_be_an_object");
        requireExactFields(
                root,
                "unexpected_or_missing_fields",
                "protocolVersion",
                "commandId",
                "type",
                "arguments",
                "timeoutMs"
        );
        requireProtocolVersion(root.get("protocolVersion"));

        String commandId = requireIdentifier(
                root.get("commandId"),
                "invalid_command_id"
        );
        BodyProtocolCommand.Type type = parseCommandType(root.get("type"));
        int timeoutMs = requireInteger(root.get("timeoutMs"), "invalid_timeout");
        if (timeoutMs < MIN_TIMEOUT_MS || timeoutMs > MAX_TIMEOUT_MS) {
            throw new BodyProtocolException("invalid_timeout");
        }

        JsonElement argumentValue = root.get("arguments");
        if (argumentValue == null || !argumentValue.isJsonObject()) {
            throw new BodyProtocolException("arguments_must_be_an_object");
        }
        JsonObject arguments = argumentValue.getAsJsonObject();
        Integer stepCount = null;
        String routineId = null;
        if (type == BodyProtocolCommand.Type.MOVE_STEPS) {
            requireExactFields(
                    arguments,
                    "move_steps_requires_count",
                    "count"
            );
            stepCount = requireInteger(
                    arguments.get("count"),
                    "count_must_be_an_integer"
            );
        } else if (type == BodyProtocolCommand.Type.DANCE) {
            requireExactFields(
                    arguments,
                    "dance_requires_routine_id",
                    "routineId"
            );
            routineId = requireIdentifier(
                    arguments.get("routineId"),
                    "invalid_routine_id"
            );
        } else if (arguments.size() != 0) {
            throw new BodyProtocolException("command_does_not_accept_arguments");
        }

        return BodyProtocolCommand.decoded(
                commandId,
                type,
                stepCount,
                routineId,
                timeoutMs
        );
    }

    byte[] encodeEvent(BodyProtocolEvent event) throws BodyProtocolException {
        JsonObject root = new JsonObject();
        root.addProperty("protocolVersion", BodyCapabilities.PROTOCOL_VERSION);
        root.addProperty("commandId", event.getCommandId());
        root.addProperty("type", event.getType().name());
        JsonObject data = new JsonObject();

        switch (event.getType()) {
            case CAPABILITIES:
                BodyCapabilities capabilities = event.getCapabilities();
                data.addProperty("protocolVersion", capabilities.getProtocolVersion());
                data.addProperty(
                        "maxStepsPerCommand",
                        capabilities.getMaxStepsPerCommand()
                );
                JsonArray routines = new JsonArray();
                for (String routineId : capabilities.getRoutineIds()) {
                    routines.add(routineId);
                }
                data.add("routineIds", routines);
                data.addProperty("supportsStop", capabilities.supportsStop());
                data.addProperty("linkWatchdogMs", capabilities.getLinkWatchdogMs());
                data.addProperty("servoCount", capabilities.getServoCount());
                break;
            case ALIVE:
                data.addProperty("moving", event.isMoving());
                break;
            case ACCEPTED:
                data.addProperty("action", event.getAction().name());
                data.addProperty(
                        "estimatedDurationMs",
                        event.getEstimatedDurationMs()
                );
                break;
            case COMPLETED:
                data.addProperty("action", event.getAction().name());
                break;
            case STOPPED:
                data.addProperty("reason", event.getReason());
                if (event.getStoppedCommandId() == null) {
                    data.add("stoppedCommandId", JsonNull.INSTANCE);
                } else {
                    data.addProperty(
                            "stoppedCommandId",
                            event.getStoppedCommandId()
                    );
                }
                break;
            case REJECTED:
            default:
                data.addProperty("reason", event.getReason());
                break;
        }
        root.add("data", data);

        byte[] payload = encode(root);
        decodeEvent(payload);
        return payload;
    }

    BodyProtocolEvent decodeEvent(byte[] payload) throws BodyProtocolException {
        JsonObject root = decodeObject(payload, "event_must_be_an_object");
        requireExactFields(
                root,
                "unexpected_or_missing_event_fields",
                "protocolVersion",
                "commandId",
                "type",
                "data"
        );
        requireProtocolVersion(root.get("protocolVersion"));
        String commandId = requireIdentifier(
                root.get("commandId"),
                "invalid_command_id"
        );
        BodyProtocolEvent.Type type = parseEventType(root.get("type"));
        JsonElement dataValue = root.get("data");
        if (dataValue == null || !dataValue.isJsonObject()) {
            throw new BodyProtocolException("data_must_be_an_object");
        }
        JsonObject data = dataValue.getAsJsonObject();

        switch (type) {
            case CAPABILITIES:
                return BodyProtocolEvent.capabilities(
                        commandId,
                        decodeCapabilities(data)
                );
            case ALIVE:
                requireExactFields(data, "invalid_alive_data", "moving");
                return BodyProtocolEvent.alive(
                        commandId,
                        requireBoolean(data.get("moving"), "invalid_alive_data")
                );
            case ACCEPTED:
                requireExactFields(
                        data,
                        "invalid_accepted_data",
                        "action",
                        "estimatedDurationMs"
                );
                long durationMs = requireInteger(
                        data.get("estimatedDurationMs"),
                        "invalid_accepted_data"
                );
                if (durationMs <= 0L || durationMs > MAX_TIMEOUT_MS) {
                    throw new BodyProtocolException("invalid_accepted_data");
                }
                return BodyProtocolEvent.accepted(
                        commandId,
                        requireMotionAction(data.get("action")),
                        durationMs
                );
            case COMPLETED:
                requireExactFields(data, "invalid_completed_data", "action");
                return BodyProtocolEvent.completed(
                        commandId,
                        requireMotionAction(data.get("action"))
                );
            case STOPPED:
                requireExactFields(
                        data,
                        "invalid_stopped_data",
                        "reason",
                        "stoppedCommandId"
                );
                return BodyProtocolEvent.stopped(
                        commandId,
                        requireString(data.get("reason"), "invalid_stopped_data"),
                        requireNullableIdentifier(data.get("stoppedCommandId"))
                );
            case REJECTED:
            default:
                requireExactFields(data, "invalid_rejected_data", "reason");
                return BodyProtocolEvent.rejected(
                        commandId,
                        requireString(data.get("reason"), "invalid_rejected_data")
                );
        }
    }

    private static BodyCapabilities decodeCapabilities(JsonObject data)
            throws BodyProtocolException {
        requireExactFields(
                data,
                "invalid_capabilities_data",
                "protocolVersion",
                "maxStepsPerCommand",
                "routineIds",
                "supportsStop",
                "linkWatchdogMs",
                "servoCount"
        );
        requireProtocolVersion(data.get("protocolVersion"));
        int maxSteps = requireInteger(
                data.get("maxStepsPerCommand"),
                "invalid_capabilities_data"
        );
        int watchdogMs = requireInteger(
                data.get("linkWatchdogMs"),
                "invalid_capabilities_data"
        );
        int servoCount = requireInteger(
                data.get("servoCount"),
                "invalid_capabilities_data"
        );
        if (maxSteps < 1
                || maxSteps > 100
                || watchdogMs < MIN_TIMEOUT_MS
                || watchdogMs > MAX_TIMEOUT_MS
                || servoCount < 1
                || servoCount > 16) {
            throw new BodyProtocolException("invalid_capabilities_data");
        }

        JsonElement routineValue = data.get("routineIds");
        if (routineValue == null || !routineValue.isJsonArray()) {
            throw new BodyProtocolException("invalid_capabilities_data");
        }
        Set<String> routines = new LinkedHashSet<>();
        for (JsonElement value : routineValue.getAsJsonArray()) {
            String routineId = requireIdentifier(
                    value,
                    "invalid_capabilities_data"
            );
            if (!routines.add(routineId)) {
                throw new BodyProtocolException("invalid_capabilities_data");
            }
        }
        return new BodyCapabilities(
                BodyCapabilities.PROTOCOL_VERSION,
                maxSteps,
                routines,
                requireBoolean(
                        data.get("supportsStop"),
                        "invalid_capabilities_data"
                ),
                watchdogMs,
                servoCount
        );
    }

    private static byte[] encode(JsonObject value) throws BodyProtocolException {
        byte[] payload = GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        if (payload.length > MAX_MESSAGE_BYTES) {
            throw new BodyProtocolException("message_too_large");
        }
        return payload;
    }

    private static JsonObject decodeObject(byte[] payload, String objectError)
            throws BodyProtocolException {
        if (payload == null || payload.length > MAX_MESSAGE_BYTES) {
            throw new BodyProtocolException("message_too_large");
        }

        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString();
        } catch (CharacterCodingException error) {
            throw new BodyProtocolException("invalid_json", error);
        }

        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new BodyProtocolException("invalid_json");
            }
            if (!value.isJsonObject()) {
                throw new BodyProtocolException(objectError);
            }
            return value.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException error) {
            throw new BodyProtocolException("invalid_json", error);
        } catch (java.io.IOException error) {
            throw new BodyProtocolException("invalid_json", error);
        }
    }

    private static void requireExactFields(
            JsonObject object,
            String reason,
            String... fields
    ) throws BodyProtocolException {
        if (object.size() != fields.length) {
            throw new BodyProtocolException(reason);
        }
        for (String field : fields) {
            if (!object.has(field)) {
                throw new BodyProtocolException(reason);
            }
        }
    }

    private static void requireProtocolVersion(JsonElement value)
            throws BodyProtocolException {
        if (requireInteger(value, "unsupported_protocol_version")
                != BodyCapabilities.PROTOCOL_VERSION) {
            throw new BodyProtocolException("unsupported_protocol_version");
        }
    }

    private static int requireInteger(JsonElement value, String reason)
            throws BodyProtocolException {
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new BodyProtocolException(reason);
        }
        try {
            String lexicalValue = value.getAsString();
            if (!INTEGER.matcher(lexicalValue).matches()) {
                throw new BodyProtocolException(reason);
            }
            BigDecimal decimal = new BigDecimal(lexicalValue);
            return decimal.intValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new BodyProtocolException(reason, error);
        }
    }

    private static boolean requireBoolean(JsonElement value, String reason)
            throws BodyProtocolException {
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new BodyProtocolException(reason);
        }
        return value.getAsBoolean();
    }

    private static String requireString(JsonElement value, String reason)
            throws BodyProtocolException {
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new BodyProtocolException(reason);
        }
        String text = value.getAsString();
        if (text.isEmpty() || text.length() > 64) {
            throw new BodyProtocolException(reason);
        }
        return text;
    }

    private static String requireIdentifier(JsonElement value, String reason)
            throws BodyProtocolException {
        String identifier = requireString(value, reason);
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new BodyProtocolException(reason);
        }
        return identifier;
    }

    private static String requireNullableIdentifier(JsonElement value)
            throws BodyProtocolException {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        return requireIdentifier(value, "invalid_stopped_data");
    }

    private static BodyProtocolCommand.Type parseCommandType(JsonElement value)
            throws BodyProtocolException {
        try {
            return BodyProtocolCommand.Type.valueOf(
                    requireString(value, "unknown_command_type")
            );
        } catch (IllegalArgumentException error) {
            throw new BodyProtocolException("unknown_command_type", error);
        }
    }

    private static BodyProtocolEvent.Type parseEventType(JsonElement value)
            throws BodyProtocolException {
        try {
            return BodyProtocolEvent.Type.valueOf(
                    requireString(value, "unknown_event_type")
            );
        } catch (IllegalArgumentException error) {
            throw new BodyProtocolException("unknown_event_type", error);
        }
    }

    private static BodyProtocolCommand.Type requireMotionAction(JsonElement value)
            throws BodyProtocolException {
        BodyProtocolCommand.Type action;
        try {
            action = BodyProtocolCommand.Type.valueOf(
                    requireString(value, "invalid_action")
            );
        } catch (IllegalArgumentException error) {
            throw new BodyProtocolException("invalid_action", error);
        }
        if (action != BodyProtocolCommand.Type.MOVE_STEPS
                && action != BodyProtocolCommand.Type.DANCE) {
            throw new BodyProtocolException("invalid_action");
        }
        return action;
    }
}
