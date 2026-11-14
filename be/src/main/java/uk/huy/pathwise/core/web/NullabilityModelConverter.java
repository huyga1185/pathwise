package uk.huy.pathwise.core.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.core.validation.MergePatch;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes the null convention of the API (P-48) into the generated OpenAPI document, so clients that generate their
 * types from it (P-41) know which fields are always there and which may be {@code null} (F-49, F-50).
 * <ul>
 *     <li>Response records ({@code *Response} and the records nested in one): every component is {@code required}
 *     and never {@code null}, except one marked {@link Nullable}, whose type also allows {@code null}. In a record
 *     with {@code @JsonInclude(NON_NULL)} a {@link Nullable} component is left out of the JSON instead, so it is not
 *     {@code required} and its type stays as it is.</li>
 *     <li>Merge patch requests ({@link MergePatch}): a {@link Nullable} field allows {@code null}, which clears the
 *     value (API_SPEC §1). Which fields are required is left to the validation annotations.</li>
 * </ul>
 */
class NullabilityModelConverter implements ModelConverter {
    private static final String RESPONSE_SUFFIX = "Response";

    @Override
    public Schema<?> resolve(io.swagger.v3.core.converter.AnnotatedType type, ModelConverterContext context,
                             Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) return null;
        Schema<?> resolved = chain.next().resolve(type, context, chain);
        if (resolved == null) return null;
        // A model used as a property comes back as a reference; its definition is the one to describe.
        Schema<?> model = resolved.get$ref() != null ? definition(context, resolved.get$ref()) : resolved;
        if (model == null || model.getProperties() == null) return resolved;
        Class<?> raw = Json.mapper().constructType(type.getType()).getRawClass();
        if (raw.isRecord() && isResponse(raw)) describeResponse(raw, model);
        else if (MergePatch.class.isAssignableFrom(raw)) describeMergePatch(raw, model);
        else if (raw.isRecord()) dropGroupOnlyRequired(raw, model);
        return resolved;
    }

    @SuppressWarnings("rawtypes")
    private static Schema<?> definition(ModelConverterContext context, String ref) {
        Map<String, Schema> models = context.getDefinedModels();
        return models == null ? null : models.get(ref.substring(ref.lastIndexOf('/') + 1));
    }

    private static boolean isResponse(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
            if (current.getSimpleName().endsWith(RESPONSE_SUFFIX)) return true;
        }
        return false;
    }

    /**
     * A request field whose "required" constraint only applies to a validation group other than {@link Default} is
     * not required by every endpoint that takes the record ({@code PlanDraftRequest.version}: {@code PUT} only, F-51).
     */
    private static void dropGroupOnlyRequired(Class<?> type, Schema<?> schema) {
        if (schema.getRequired() == null) return;
        List<String> required = new ArrayList<>(schema.getRequired());
        for (RecordComponent component : type.getRecordComponents()) {
            Class<?>[] groups = requiredGroups(component);
            if (groups != null && groups.length > 0 && !Arrays.asList(groups).contains(Default.class))
                required.remove(component.getName());
        }
        schema.setRequired(required.isEmpty() ? null : required);
    }

    /**
     * @return the groups of the component's {@code @NotNull}, {@code @NotBlank} or {@code @NotEmpty}, null without one
     */
    private static Class<?> @Nullable [] requiredGroups(RecordComponent component) {
        Field field;
        try {
            field = component.getDeclaringRecord().getDeclaredField(component.getName());
        } catch (NoSuchFieldException e) {
            return null;
        }
        NotNull notNull = field.getAnnotation(NotNull.class);
        if (notNull != null) return notNull.groups();
        NotBlank notBlank = field.getAnnotation(NotBlank.class);
        if (notBlank != null) return notBlank.groups();
        NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
        return notEmpty == null ? null : notEmpty.groups();
    }

    private static void describeResponse(Class<?> type, Schema<?> schema) {
        boolean omitsNulls = omitsNulls(type);
        List<String> required = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            String name = component.getName();
            if (!schema.getProperties().containsKey(name)) continue;
            boolean nullable = isNullable(component.getAnnotatedType(), component);
            if (nullable && omitsNulls) continue;
            required.add(name);
            if (nullable) allowNull(schema, name);
        }
        schema.setRequired(required.isEmpty() ? null : required);
    }

    private static void describeMergePatch(Class<?> type, Schema<?> schema) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (schema.getProperties().containsKey(field.getName()) && isNullable(field.getAnnotatedType(), field)) {
                    allowNull(schema, field.getName());
                }
            }
        }
    }

    private static boolean omitsNulls(Class<?> type) {
        JsonInclude include = type.getAnnotation(JsonInclude.class);
        return include != null && include.value() == JsonInclude.Include.NON_NULL;
    }

    /** JSpecify's {@code @Nullable} is a type annotation; it is also accepted on the element itself. */
    private static boolean isNullable(AnnotatedType annotatedType, AnnotatedElement element) {
        return annotatedType.isAnnotationPresent(Nullable.class) || element.isAnnotationPresent(Nullable.class);
    }

    /**
     * Lets property {@code name} be {@code null}: {@code null} joins its {@code type}, or a reference to another schema
     * becomes {@code anyOf: [the reference, {type: null}]} (OpenAPI 3.1, JSON Schema).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void allowNull(Schema<?> schema, String name) {
        Map<String, Schema> properties = new LinkedHashMap<>(schema.getProperties());
        Schema property = properties.get(name);
        if (property.get$ref() != null) {
            Schema reference = new JsonSchema().$ref(property.get$ref());
            Schema nullType = new JsonSchema().types(Set.of("null"));
            properties.put(name, new JsonSchema().anyOf(List.of(reference, nullType)));
        } else {
            Set<String> types = new LinkedHashSet<>(property.getTypes() != null ? property.getTypes() : Set.of());
            if (types.isEmpty() && property.getType() != null) types.add(property.getType());
            types.add("null");
            property.setTypes(types);
        }
        schema.setProperties(properties);
    }
}
