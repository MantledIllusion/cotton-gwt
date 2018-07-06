package com.mantledillusion.vaadin.cotton.conversion;

public interface Converter<SourceType, TargetType> {

	TargetType toTarget(SourceType source, ConversionService service) throws Exception;
}
