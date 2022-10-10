/*
 * Copyright 2010 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.javakaffee.kryoserializers;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo {@link Serializer}s for lists created via {@link List#subList(int, int)}.
 * An instance of a serializer can be obtained via {@link #createFor(Class)}, which
 * just returns <code>null</code> if the given type is not supported by these
 * serializers.
 * 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class SubListSerializers {

    static Class<?> getClass(final String className) {
        try {
            return Class.forName(className);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Class<?> getClassOrNull(final String className) {
        try {
            return Class.forName(className);
        } catch (final Exception e) {
            return null;
        }
    }
    
    // Workaround reference reading, this should be removed sometimes. See also
    // https://groups.google.com/d/msg/kryo-users/Eu5V4bxCfws/k-8UQ22y59AJ
    private static final Object FAKE_REFERENCE = new Object();

    /**
     * Obtain a serializer for the given sublist type. If the type is not supported
     * <code>null</code> is returned.
     * @param type the class of the sublist.
     * @return a serializer instance or <code>null</code>.
     */
    @SuppressWarnings("rawtypes")
    public static Serializer<List<?>> createFor(final Class type) {
        if (ArrayListSubListSerializer.canSerialize(type))
            return new ArrayListSubListSerializer();
        if (JavaUtilSubListSerializer.canSerialize(type))
            return new JavaUtilSubListSerializer();
        return null;
    }

    /**
     * Adds appropriate sublist serializers as default serializers.
     */
    public static Kryo addDefaultSerializers(Kryo kryo) {
        ArrayListSubListSerializer.addDefaultSerializer(kryo);
        AbstractListSubListSerializer.addDefaultSerializer(kryo);
        JavaUtilSubListSerializer.addDefaultSerializer(kryo);
        return kryo;
    }

    /**
     * Supports sublists created via {@link ArrayList#subList(int, int)} since java7 and {@link LinkedList#subList(int, int)} since java9 (openjdk).
     */
    private static class SubListSerializer extends Serializer<List<?>> {

        private Field _parentField;
        private Field _parentOffsetField;
        private Field _sizeField;

        public SubListSerializer(String subListClassName) {
            try {
                final Class<?> clazz = Class.forName(subListClassName);
                _parentField = getParentField(clazz);
                _parentOffsetField = getOffsetField(clazz);
                _sizeField = clazz.getDeclaredField( "size" );
                _parentField.setAccessible( true );
                _parentOffsetField.setAccessible( true );
                _sizeField.setAccessible( true );
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Field getParentField(Class clazz) throws NoSuchFieldException {
            try {
                // java 9+
                return clazz.getDeclaredField("root");
            } catch(NoSuchFieldException e) {
                return clazz.getDeclaredField("parent");
            }
        }

        private static Field getOffsetField(Class<?> clazz) throws NoSuchFieldException {
            try {
                // up to jdk8 (which also has an "offset" field (we don't need) - therefore we check "parentOffset" first
                return clazz.getDeclaredField( "parentOffset" );
            } catch (NoSuchFieldException e) {
                // jdk9+ only has "offset" which is the parent offset
                return clazz.getDeclaredField( "offset" );
            }
        }

        @Override
        public List<?> read(final Kryo kryo, final Input input, final Class<? extends List<?>> clazz) {
            kryo.reference(FAKE_REFERENCE);
            final List<?> list = (List<?>) kryo.readClassAndObject(input);
            final int fromIndex = input.readInt(true);
            final int toIndex = input.readInt(true);
            return list.subList(fromIndex, toIndex);
        }

        @Override
        public void write(final Kryo kryo, final Output output, final List<?> obj) {
            try {
                kryo.writeClassAndObject(output, _parentField.get(obj));
                final int parentOffset = _parentOffsetField.getInt( obj );
                final int fromIndex = parentOffset;
                output.writeInt(fromIndex, true);
                final int toIndex = fromIndex + _sizeField.getInt( obj );
                output.writeInt(toIndex, true);
            } catch (final RuntimeException e) {
                // Don't eat and wrap RuntimeExceptions because the ObjectBuffer.write...
                // handles SerializationException specifically (resizing the buffer)...
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<?> copy(final Kryo kryo, final List<?> original) {
            kryo.reference(FAKE_REFERENCE);
            try {
                final List<?> list = (List<?>) _parentField.get(original);
                final int parentOffset = _parentOffsetField.getInt( original );
                final int fromIndex = parentOffset;
                final int toIndex = fromIndex + _sizeField.getInt( original );
                return kryo.copy(list).subList(fromIndex, toIndex);
            } catch(final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Supports sublists created via {@link ArrayList#subList(int, int)} since java7 (oracle jdk,
     * represented by <code>java.util.ArrayList$SubList</code>).
     */
    public static class ArrayListSubListSerializer extends Serializer<List<?>> {

        public static final Class<?> SUBLIST_CLASS = SubListSerializers.getClassOrNull("java.util.ArrayList$SubList");

        private final SubListSerializer delegate = new SubListSerializer("java.util.ArrayList$SubList");

        /**
         * Can be used to determine, if the given type can be handled by this serializer.
         * 
         * @param type
         *            the class to check.
         * @return <code>true</code> if the given class can be serialized/deserialized by this serializer.
         */
        public static boolean canSerialize(final Class<?> type) {
            return SUBLIST_CLASS != null && SUBLIST_CLASS.isAssignableFrom(type);
        }

        public static Kryo addDefaultSerializer(Kryo kryo) {
            if(SUBLIST_CLASS != null) kryo.addDefaultSerializer(SUBLIST_CLASS, new ArrayListSubListSerializer());
            return kryo;
        }

        @Override
        public List<?> read(final Kryo kryo, final Input input, final Class<? extends List<?>> clazz) {
            return delegate.read(kryo, input, clazz);
        }

        @Override
        public void write(final Kryo kryo, final Output output, final List<?> obj) {
            delegate.write(kryo, output, obj);
        }

        @Override
        public List<?> copy(final Kryo kryo, final List<?> original) {
            return delegate.copy(kryo, original);
        }
    }

    /**
     * Supports sublists created via {@link LinkedList#subList(int, int)} since java9 (oracle jdk,
     * represented by <code>java.util.AbstractList$SubList</code>).
     */
    public static class AbstractListSubListSerializer extends Serializer<List<?>> {

        public static final Class<?> SUBLIST_CLASS = SubListSerializers.getClassOrNull("java.util.AbstractList$SubList");

        private final SubListSerializer delegate = new SubListSerializer("java.util.AbstractList$SubList");

        /**
         * Can be used to determine, if the given type can be handled by this serializer.
         *
         * @param type
         *            the class to check.
         * @return <code>true</code> if the given class can be serialized/deserialized by this serializer.
         */
        public static boolean canSerialize(final Class<?> type) {
            return SUBLIST_CLASS != null && SUBLIST_CLASS.isAssignableFrom(type);
        }

        public static Kryo addDefaultSerializer(Kryo kryo) {
            if(SUBLIST_CLASS != null) kryo.addDefaultSerializer(SUBLIST_CLASS, new AbstractListSubListSerializer());
            return kryo;
        }

        @Override
        public List<?> read(final Kryo kryo, final Input input, final Class<? extends List<?>> clazz) {
            return delegate.read(kryo, input, clazz);
        }

        @Override
        public void write(final Kryo kryo, final Output output, final List<?> obj) {
            delegate.write(kryo, output, obj);
        }

        @Override
        public List<?> copy(final Kryo kryo, final List<?> original) {
            return delegate.copy(kryo, original);
        }
    }

    /**
     * Supports sublists created via {@link AbstractList#subList(int, int)}, e.g. LinkedList.
     * In oracle jdk such sublists are represented by <code>java.util.SubList</code>.
     */
    public static class JavaUtilSubListSerializer extends Serializer<List<?>> {

        public static final Class<?> SUBLIST_CLASS = SubListSerializers.getClassOrNull("java.util.SubList");

        private Field _listField;
        private Field _offsetField;
        private Field _sizeField;

        public JavaUtilSubListSerializer() {
            try {
                final Class<?> clazz = Class.forName("java.util.SubList");
                _listField = clazz.getDeclaredField("l");
                _offsetField = clazz.getDeclaredField("offset");
                _sizeField = clazz.getDeclaredField("size");
                _listField.setAccessible(true);
                _offsetField.setAccessible(true);
                _sizeField.setAccessible(true);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Can be used to determine, if the given type can be handled by this serializer.
         * 
         * @param type
         *            the class to check.
         * @return <code>true</code> if the given class can be serialized/deserialized by this serializer.
         */
        public static boolean canSerialize(final Class<?> type) {
            return SUBLIST_CLASS != null && SUBLIST_CLASS.isAssignableFrom(type);
        }

        public static Kryo addDefaultSerializer(Kryo kryo) {
            if(SUBLIST_CLASS != null) kryo.addDefaultSerializer(SUBLIST_CLASS, new JavaUtilSubListSerializer());
            return kryo;
        }

        @Override
        public List<?> read(final Kryo kryo, final Input input, final Class<? extends List<?>> clazz) {
            kryo.reference(FAKE_REFERENCE);
            final List<?> list = (List<?>) kryo.readClassAndObject(input);
            final int fromIndex = input.readInt(true);
            final int toIndex = input.readInt(true);
            return list.subList(fromIndex, toIndex);
        }

        @Override
        public void write(final Kryo kryo, final Output output, final List<?> obj) {
            try {
                kryo.writeClassAndObject(output, _listField.get(obj));
                final int fromIndex = _offsetField.getInt(obj);
                output.writeInt(fromIndex, true);
                final int toIndex = fromIndex + _sizeField.getInt(obj);
                output.writeInt(toIndex, true);
            } catch (final RuntimeException e) {
                // Don't eat and wrap RuntimeExceptions because the ObjectBuffer.write...
                // handles SerializationException specifically (resizing the buffer)...
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<?> copy(final Kryo kryo, final List<?> obj) {
            kryo.reference(FAKE_REFERENCE);
            try {
                final List<?> list = (List<?>) _listField.get(obj);
                final int fromIndex = _offsetField.getInt(obj);
                final int toIndex = fromIndex + _sizeField.getInt(obj);
                return kryo.copy(list).subList(fromIndex, toIndex);
            } catch(final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
