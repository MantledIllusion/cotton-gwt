package com.mantledillusion.vaadin.cotton.conversion;

public interface Mapper<SourceType, TargetType> extends Converter<SourceType, TargetType> {

	@Override
	default TargetType toTarget(SourceType source, ConversionService service) {
		TargetType target = fetchTarget(source);
		map(source, target, service);
		return target;
	}
	
	TargetType fetchTarget(SourceType source);
	
	void map(SourceType source, TargetType target, ConversionService service);
}
