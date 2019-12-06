//package com.syb.coffeelint.utils;
//
//import com.syb.coffeelint.type.Type;
//
//import java.lang.reflect.AccessibleObject;
//import java.lang.reflect.Array;
//import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import sun.reflect.ConstructorAccessor;
//import sun.reflect.FieldAccessor;
//import sun.reflect.ReflectionFactory;
//
//public class EnumHelper {
//
//    private static final String MODIFIERS_FIELD = "modifiers";
//
//    private static ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();
//
//    private static void setFailsafeFieldValue(Field field, Object target, Object value) throws NoSuchFieldException,
//            IllegalAccessException {
//
//        // let's make the field accessible
//        field.setAccessible(true);
//        // next we change the modifier in the Field instance to
//        // not be final anymore, thus tricking reflection into
//        // letting us modify the static final field
//        Field modifiersField = Field.class.getDeclaredField(MODIFIERS_FIELD);
//        modifiersField.setAccessible(true);
//        int modifiers = modifiersField.getInt(field);
//
//        // blank out the final bit in the modifiers int
//        modifiers &= ~Modifier.FINAL;
//        modifiersField.setInt(field, modifiers);
//
//        FieldAccessor fa = reflectionFactory.newFieldAccessor(field, false);
//        fa.set(target, value);
//    }
//
//    private static void blankField(Class<?> enumClass, String fieldName) throws NoSuchFieldException,
//            IllegalAccessException {
//        for (Field field : Class.class.getDeclaredFields()) {
//            if (field.getName().contains(fieldName)) {
//                AccessibleObject.setAccessible(new Field[] { field }, true);
//                setFailsafeFieldValue(field, enumClass, null);
//                break;
//            }
//        }
//    }
//
//    private static void cleanEnumCache(Class<?> enumClass) throws NoSuchFieldException, IllegalAccessException {
//        blankField(enumClass, "enumConstantDirectory"); // Sun (Oracle?!?) JDK 1.5/6
//        blankField(enumClass, "enumConstants"); // IBM JDK
//    }
//    private static ConstructorAccessor getConstructorAccessor(Class<?> enumClass, Class<?>[] additionalParameterTypes)
//            throws NoSuchMethodException {
//        Class<?>[] parameterTypes = new Class[additionalParameterTypes.length + 2];
//        parameterTypes[0] = String.class;
//        parameterTypes[1] = int.class;
//        System.arraycopy(additionalParameterTypes, 0, parameterTypes, 2, additionalParameterTypes.length);
//        return reflectionFactory.newConstructorAccessor(enumClass.getDeclaredConstructor(parameterTypes));
//    }
//
//    private static Object makeEnum(Class<?> enumClass, String value, int ordinal, Class<?>[] additionalTypes,
//                                   Object[] additionalValues) throws Exception {
//        Object[] parms = new Object[additionalValues.length + 2];
//        parms[0] = value;
//        parms[1] = ordinal;
//        System.arraycopy(additionalValues, 0, parms, 2, additionalValues.length);
//        return enumClass.cast(getConstructorAccessor(enumClass, additionalTypes).newInstance(parms));
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <T extends Enum<?>> void addEnum(Class<T> enumType, String enumName) {
//        addEnum(enumType, enumName, new Class[0], new Object[0]);
//    }
//
//    /**
//     * Add an enum instance to the enum class given as argument
//     *
//     * @param <T> the type of the enum (implicit)
//     * @param enumType the class of the enum to be modified
//     * @param enumName the name of the new enum instance to be added to the class.
//     */
//    @SuppressWarnings("unchecked")
//    public static <T extends Enum<?>> void addEnum(Class<T> enumType, String enumName, Class<?>[] paramClass, Object[] paramValue) {
//
//        // 0. Sanity checks
//        if (!Enum.class.isAssignableFrom(enumType)) {
//            throw new RuntimeException("class " + enumType + " is not an instance of Enum");
//        }
//
//        // 1. Lookup "$VALUES" holder in enum class and get previous enum instances
//        Field valuesField = null;
//        Field[] fields = Type.class.getDeclaredFields();
//        for (Field field : fields) {
//            if (field.getName().contains("$VALUES")) {
//                valuesField = field;
//                break;
//            }
//        }
//        AccessibleObject.setAccessible(new Field[] { valuesField }, true);
//
//        try {
//
//            // 2. Copy it
//            T[] previousValues = (T[]) valuesField.get(enumType);
//            List<T> values = new ArrayList<T>(Arrays.asList(previousValues));
//
//            // 3. build new enum
//            T newValue = (T) makeEnum(enumType, // The target enum class
//                    enumName, // THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
//                    values.size(),
//                    //new Class<?>[] {}, // could be used to pass values to the enum constuctor if needed
//                    paramClass,
//                    //new Object[] {}
//                    paramValue
//            ); // could be used to pass values to the enum constuctor if needed
//
//            // 4. add new value
//            values.add(newValue);
//            Object object = values.toArray((T[]) Array.newInstance(enumType, 0));
//            // 5. Set new values field
//            setFailsafeFieldValue(valuesField, null, object);
//
//            // 6. Clean enum cache
//            cleanEnumCache(enumType);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }
//
//}