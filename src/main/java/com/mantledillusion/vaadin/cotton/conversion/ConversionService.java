package com.mantledillusion.vaadin.cotton.conversion;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeUtils;

public class ConversionService {

	private interface Function<T, U, R> {

	    R apply(T t, U u) throws Exception;
	}
	
	private final Map<Class<?>, Map<Class<?>, Function<?, ConversionService, ?>>> converterRegistry;

	private ConversionService(Map<Class<?>, Map<Class<?>, Function<?, ConversionService, ?>>> converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

	@SuppressWarnings("unchecked")
	public <SourceType, TargetType> TargetType convert(SourceType source, Class<TargetType> targetType) {
		if (source == null) {
			return null;
		}

		if (this.converterRegistry.containsKey(targetType)) {
			Map<Class<?>, Function<?, ConversionService, ?>> targetTypeConverters = this.converterRegistry
					.get(targetType);

			Class<? super SourceType> sourceType = (Class<? super SourceType>) source.getClass();
			do {
				if (targetTypeConverters.containsKey(sourceType)) {
					try {
						return ((Function<SourceType, ConversionService, TargetType>) targetTypeConverters
								.get(sourceType)).apply(source, this);
					} catch (Exception e) {
						throw new ConversionException(e);
					}
				}
				sourceType = sourceType.getSuperclass();
			} while (sourceType != Object.class);
		}

		throw new RuntimeException("No converter for " + source.getClass().getSimpleName()
				+ " or any of its super types when converting to the target type " + targetType.getSimpleName());
	}

	public <SourceType, TargetType> List<TargetType> convertList(List<SourceType> source,
			Class<TargetType> targetType) {
		return convertInto(source, new ArrayList<>(), targetType);
	}

	public <SourceType, SourceCollectionType extends Collection<SourceType>, TargetCollectionType extends Collection<TargetType>, TargetType> TargetCollectionType convertInto(
			SourceCollectionType source, TargetCollectionType target, Class<TargetType> targetType) {
		for (SourceType sourceElement : source) {
			target.add(convert(sourceElement, targetType));
		}
		return target;
	}

	public <SourceType extends Enum<SourceType>, TargetType extends Enum<TargetType>> TargetType convertNamed(
			SourceType source, Class<TargetType> targetType) {
		return Enum.valueOf(targetType, source.name());
	}

	public static <SourceType, TargetType> ConversionService of(Collection<Converter<?, ?>> converters) {
		Map<Class<?>, Map<Class<?>, Function<?, ConversionService, ?>>> converterRegistry = new HashMap<>();

		for (Converter<?, ?> converter : converters) {
			Map<TypeVariable<?>, Type> types = TypeUtils.getTypeArguments(converter.getClass(), Converter.class);
			Class<?> sourceType = validateConverterTypeParameter(types.get(Converter.class.getTypeParameters()[0]));
			Class<?> targetType = validateConverterTypeParameter(types.get(Converter.class.getTypeParameters()[1]));

			if (!converterRegistry.containsKey(targetType)) {
				converterRegistry.put(targetType, new HashMap<>());
			}

			@SuppressWarnings("unchecked")
			Converter<SourceType, TargetType> typedConverter = (Converter<SourceType, TargetType>) converter;
			Function<SourceType, ConversionService, TargetType> function = (source, conversionService) -> typedConverter.toTarget(source, conversionService);
			converterRegistry.get(targetType).put(sourceType, function);
		}

		return new ConversionService(converterRegistry);
	}

	private static Class<?> validateConverterTypeParameter(Type typeParameter) {
		if (typeParameter instanceof Class) {
			return (Class<?>) typeParameter;
		} else if (typeParameter instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) typeParameter).getRawType();
		} else {
			throw new RuntimeException("Wrong converter generic param type");
		}
	}
}
