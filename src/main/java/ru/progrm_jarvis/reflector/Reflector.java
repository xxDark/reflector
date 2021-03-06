/*
 *  Copyright 2018 Petr P.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.progrm_jarvis.reflector;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import ru.progrm_jarvis.reflector.bytecode.asm.SafeClassDefiner;
import ru.progrm_jarvis.reflector.util.ValueContainer;
import ru.progrm_jarvis.reflector.util.function.ThrowingFunction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Main reflector class which wraps {@link Class} methods with methods which don't throw checked exceptions
 *
 * TODO: 27.07.2018 full documentation
 */
@UtilityClass
@SuppressWarnings({"unused", "WeakerAccess"})
public class Reflector {

    @Getter private static final SafeClassDefiner SAFE_CLASS_DEFINER = new SafeClassDefiner();

    ///////////////////////////////////////////////////////////////////////////
    // Digging
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Digs for constructor following the given condition in class specified and all its parents until the bound.
     *
     * @param clazz class from which to start the search
     * @param condition condition by which to check each constructor
     * @param bound bounding of class, the one after reaching of which digging ends (inclusive)
     * @param <T> type of class being digged
     * @return {@link Optional} of found constructor or {@link Optional#empty()} if none was found
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> Optional<ClassMember<? super T, Constructor<? super T>>> digForConstructor(
            @NonNull final Class<T> clazz,
            @NonNull final Predicate<Constructor<? super T>> condition,
            @NonNull final Class<? super T> bound
    ) {
        return RecursiveClassDigger
                .dig(clazz, (ThrowingFunction<Class<? super T>, ValueContainer<Constructor<? super T>>, Throwable>) owner -> {
                        for (val constructor : owner.getDeclaredConstructors()) {
                            val genericConstructor = (Constructor<? super T>) constructor;

                            if (condition.test(genericConstructor)) return ValueContainer.of(genericConstructor);
                        }
                    return null;
                }, bound);
    }

    /*
    public ClassGenerator newAsmClassGenerator(){
        return newAsmClassGenerator(SAFE_CLASS_DEFINER, null);
    }

    public ClassGenerator newAsmClassGenerator(final ClassDefiner classDefiner){
        return newAsmClassGenerator(classDefiner, null);
    }

    public ClassGenerator newAsmClassGenerator(final ClassLoader classLoader){
        return newAsmClassGenerator(SAFE_CLASS_DEFINER, classLoader);
    }

    public ClassGenerator newAsmClassGenerator(@NonNull final ClassDefiner classDefiner,
                                               @Nullable final ClassLoader classLoader){
        return new AsmClassGenerator(classDefiner, classLoader);
    }
    */

    /**
     * Digs for field following the given condition in class specified and all its parents until the bound.
     *
     * @param clazz class from which to start the search
     * @param condition condition by which to check each field
     * @param bound bounding of class, the one after reaching of which digging ends (inclusive)
     * @param <T> type of class being digged
     * @return {@link Optional} of found field or {@link Optional#empty()} if none was found
     */
    @SneakyThrows
    public <T> Optional<ClassMember<? super T, Field>> digForField(
            @NonNull final Class<T> clazz,
            @NonNull final Predicate<Field> condition,
            @NonNull final Class<? super T> bound
    ) {
        return RecursiveClassDigger
                .dig(clazz, (ThrowingFunction<Class<? super T>, ValueContainer<Field>, Throwable>) owner -> {
                    for (val field : owner.getDeclaredFields()) if (condition
                            .test(field)) return ValueContainer.of(field);
                    return null;
                    }, bound);
    }

    /**
     * Digs for method following the given condition in class specified and all its parents until the bound.
     *
     * @param clazz class from which to start the search
     * @param condition condition by which to check each method
     * @param bound bounding of class, the one after reaching of which digging ends (inclusive)
     * @return {@link Optional} of found method or {@link Optional#empty()} if none was found
     */
    @SneakyThrows
    public Optional<ClassMember<?, Method>> digForMethod(
            @NonNull final Class<?> clazz,
            @NonNull final Predicate<Method> condition,
            @NonNull final Class<?> bound
    ) {
        return RecursiveClassDigger
                .digWithInterfaces(clazz, (ThrowingFunction<Class<?>, ValueContainer<Method>, Throwable>) owner -> {
                    for (val method : owner.getDeclaredMethods()) if (condition.test(method)) return ValueContainer
                            .of(method);
                    return null;
                    }, bound);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Class
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Gets the type-safe class object by its fully qualified name.
     *
     * @param className the fully qualified name of class (<i><b>PACKAGE</b>.<b>NAME</b></i>)
     * @param <T> type of class
     * @return class found by its fully qualified name
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> Class<T> classForName(@NonNull final String className) {
        return (Class<T>) Class.forName(className);
    }

    /**
     * Returns a type-safe class instance from object specified.
     *
     * @param object object of which to get class
     * @param <T> type of object and so its class
     * @return type-safe class instance of object
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> classOf(@NonNull final T object) {
        return (Class<T>) object.getClass();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Field
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Gets field wrapper for class's field available via {@link Class#getField(String)} in the class specified.
     *
     * @param clazz class in which to look for field
     * @param name name of field to get
     * @param <T> type of class containing field
     * @param <R> type of field value
     * @return field wrapper for object's field
     */
    @SneakyThrows
    public <T, R> Field getField(@NonNull final Class<T> clazz, @NonNull final String name) {
        return clazz.getField(name);
    }

    /**
     * Gets field wrapper for class's field available
     * via {@link Class#getField(String)}in the class of object specified.
     *
     * @param object object in whose class to look for field
     * @param name name of field to get
     * @param <T> type of class containing field
     * @param <R> type of field value
     * @return field wrapper for object's field
     */
    public <T, R> Field getField(@NonNull final T object, @NonNull final String name) {
        return getField(classOf(object), name);
    }

    /**
     * Gets field wrapper for class's field available via {@link Class#getDeclaredField(String)} in the class specified.
     *
     * @param clazz class in which to look for field
     * @param name name of field to get
     * @param <T> type of class containing field
     * @param <R> type of field value
     * @return field wrapper for object's field
     */
    @SneakyThrows
    public <T, R> Field getDeclaredField(@NonNull final Class<T> clazz, @NonNull final String name) {
        return clazz.getDeclaredField(name);
    }

    /**
     * Gets field wrapper for class's field available
     * via {@link Class#getDeclaredField(String)}in the class of object specified.
     *
     * @param object object in whose class to look for field
     * @param name name of field to get
     * @param <T> type of class containing field
     * @param <R> type of field value
     * @return field wrapper for object's field
     */
    public <T, R> Field getDeclaredField(@NonNull final T object, @NonNull final String name) {
        return getDeclaredField(classOf(object), name);
    }

    public <T, R> Optional<Field> getFieldOptional(@NonNull final Class<T> clazz, @NonNull final Predicate<Field> predicate) {
        return digForField(clazz, predicate, Object.class).map(ClassMember::getValue);
    }

    public <T, R> Optional<Field> getFieldOptional(@NonNull final T object, @NonNull final Predicate<Field> predicate) {
        return getFieldOptional(classOf(object), predicate);
    }

    @SuppressWarnings("ConstantConditions")
    public <T, R> Field getField(@NonNull final Class<T> clazz, @NonNull final Predicate<Field> predicate) {
        return digForField(clazz, predicate, Object.class).orElseThrow(NoSuchElementException::new).getValue();
    }

    public <T, R> Field getField(@NonNull final T object, @NonNull final Predicate<Field> predicate) {
        return getField(classOf(object), predicate);
    }

    public <T, R> Optional<Field> getAnyFieldOptional(@NonNull final Class<T> clazz, @NonNull final String name) {
        return getFieldOptional(clazz, field -> name.equals(field.getName()));
    }

    public <T, R> Optional<Field> getAnyFieldOptional(@NonNull final T object, @NonNull final String name) {
        return getAnyFieldOptional(classOf(object), name);
    }

    public <T, R> Field getAnyField(@NonNull final Class<T> clazz, @NonNull final String name) {
        return getField(clazz, field -> name.equals(field.getName()));
    }

    public <T, R> Field getAnyField(@NonNull final T object, @NonNull final String name) {
        return getAnyField(classOf(object), name);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Method
    ///////////////////////////////////////////////////////////////////////////

    @SneakyThrows
    public <T, R> Method getMethod(@NonNull final Class<T> clazz, @NonNull final String name,
                                   @NonNull final Class<?>... parameterTypes) {
        return clazz.getMethod(name, parameterTypes);
    }

    public <T, R> Method getMethod(@NonNull final T object, @NonNull final String name,
                                   @NonNull final Class<?>... parameterTypes) {
        return getMethod(classOf(object), name, parameterTypes);
    }

    @SneakyThrows
    public <T, R> Method getDeclaredMethod(@NonNull final Class<T> clazz, @NonNull final String name,
                                           @NonNull final Class<?>... parameterTypes) {
        return clazz.getDeclaredMethod(name, parameterTypes);
    }

    public <T, R> Method getDeclaredMethod(@NonNull final T object, @NonNull final String name,
                                           @NonNull final Class<?>... parameterTypes) {
        return getDeclaredMethod(classOf(object), name, parameterTypes);
    }

    public <T, R> Optional<Method> getMethodOptional(@NonNull final Class<T> clazz,
                                                     @NonNull final Predicate<Method> predicate) {
        return digForMethod(clazz, predicate, Object.class).map(ClassMember::getValue);
    }

    public <T, R> Optional<Method> getMethodOptional(@NonNull final T object,
                                                     @NonNull final Predicate<Method> predicate) {
        return getMethodOptional(classOf(object), predicate);
    }

    @SuppressWarnings("ConstantConditions")
    public <T, R> Method getMethod(@NonNull final Class<T> clazz, @NonNull final Predicate<Method> predicate) {
        return digForMethod(clazz, predicate, Object.class).orElseThrow(NullPointerException::new).getValue();
    }

    public <T, R> Method getMethod(@NonNull final T object, @NonNull final Predicate<Method> predicate) {
        return getMethod(classOf(object), predicate);
    }

    public <T, R> Optional<Method> getAnyMethodOptional(@NonNull final Class<T> clazz, @NonNull final String name) {
        return getMethodOptional(clazz, field -> name.equals(field.getName()));
    }

    public <T, R> Optional<Method> getAnyMethodOptional(@NonNull final T object, @NonNull final String name) {
        return getAnyMethodOptional(classOf(object), name);
    }

    public <T, R> Method getAnyMethod(@NonNull final Class<T> clazz, @NonNull final String name) {
        return getMethod(clazz, field -> name.equals(field.getName()));
    }

    public <T, R> Method getAnyMethod(@NonNull final T object, @NonNull final String name) {
        return getAnyMethod(classOf(object), name);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////

    @SneakyThrows
    public <T> Constructor<T> getConstructor(@NonNull final Class<T> clazz, @NonNull final Class<?>... parameterTypes) {
        return clazz.getConstructor(parameterTypes);
    }

    public <T> Constructor<T> getConstructor(@NonNull final T object, @NonNull final Class<?>... parameterTypes) {
        return getConstructor(classOf(object), parameterTypes);
    }

    @SneakyThrows
    public <T> Constructor<T> getDeclaredConstructor(@NonNull final Class<T> clazz,
                                                     @NonNull final Class<?>... parameterTypes) {
        return clazz.getDeclaredConstructor(parameterTypes);
    }

    public <T> Constructor<T> getDeclaredConstructor(@NonNull final T object,
                                                     @NonNull final Class<?>... parameterTypes) {
        return getDeclaredConstructor(classOf(object), parameterTypes);
    }

    public <T> Optional<Constructor<? super T>> getConstructorOptional(@NonNull final Class<T> clazz,
                                                                       @NonNull final Predicate<Constructor<? super T>>
                                                                        predicate) {
        return digForConstructor(clazz, predicate, Object.class).map(ClassMember::getValue);
    }

    public <T> Optional<Constructor<? super T>> getConstructorOptional(@NonNull final T object,
                                                                       @NonNull final Predicate<Constructor<? super T>>
                                                                               predicate) {
        return getConstructorOptional(classOf(object), predicate);
    }

    @SuppressWarnings("ConstantConditions")
    public <T> Constructor<? super T> getConstructor(@NonNull final Class<T> clazz,
                                                     @NonNull final Predicate<Constructor<? super T>> predicate) {
        return digForConstructor(clazz, predicate, Object.class).orElseThrow(NullPointerException::new).getValue();
    }

    public <T> Constructor<? super T> getConstructor(@NonNull final T object,
                                                     @NonNull final Predicate<Constructor<? super T>> predicate) {
        return getConstructor(classOf(object), predicate);
    }

    public <T> Optional<Constructor<? super T>> getAnyConstructorOptional(@NonNull final Class<T> clazz,
                                                                          @NonNull final Class<?>... parameterTypes) {
        return getConstructorOptional(clazz, (Predicate<Constructor<? super T>>)
                constructor -> Arrays.equals(parameterTypes, constructor.getParameterTypes()));
    }

    public <T> Optional<Constructor<? super T>> getAnyConstructorOptional(@NonNull final T object,
                                                                          @NonNull final Class<?>... parameterTypes) {
        return getAnyConstructorOptional(classOf(object), parameterTypes);
    }

    public <T> Constructor<? super T> getAnyConstructor(@NonNull final Class<T> clazz,
                                                        @NonNull final Class<?>... parameterTypes) {
        return getConstructor(clazz, (Predicate<Constructor<? super T>>)
                constructor -> Arrays.equals(parameterTypes, constructor.getParameterTypes()));
    }

    public <T> Constructor<? super T> getAnyConstructor(@NonNull final T object,
                                                        @NonNull final Class<?>... parameterTypes) {
        return getAnyConstructor(classOf(object), parameterTypes);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Unsafe
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Allocates new instance using {@link sun.misc.Unsafe} so that its constructor is not invoked.
     *
     * @param clazz class instance of which to create
     * @param <T> type of object
     * @return newly allocated instance of class
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T newUnsafeInstance(@NonNull final Class<T> clazz) {
        return (T) UnsafeUtil.UNSAFE.allocateInstance(clazz);
    }
}
