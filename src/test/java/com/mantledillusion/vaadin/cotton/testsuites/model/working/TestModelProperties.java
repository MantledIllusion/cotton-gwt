package com.mantledillusion.vaadin.cotton.testsuites.model.working;

import com.mantledillusion.data.epiphy.ModelProperty;
import com.mantledillusion.data.epiphy.ModelPropertyList;
import com.mantledillusion.data.epiphy.ReadOnlyModelProperty;

public class TestModelProperties {

	public static final ReadOnlyModelProperty<Model, Model> MODEL = ModelProperty.rootChild();

	public static final ModelProperty<Model, String> MODELID = MODEL.registerChild(model -> model.modelId, (model, value) -> {model.modelId = value;});
	
	public static final ModelPropertyList<Model, Sub> SUBLIST = MODEL.registerChildList(model -> model.subList, (model, value) -> {model.subList = value;});
	
	public static final ModelProperty<Model, Sub> SUB = SUBLIST.defineElementAsChild();

	public static final ModelProperty<Model, String> SUBID = SUB.registerChild(sub -> sub.subId, (sub, value) -> {sub.subId = value;});
	
	public static final ModelPropertyList<Model, SubSub> SUBSUBLIST = SUB.registerChildList(sub -> sub.subSubList, (sub, value) -> {sub.subSubList = value;});
	
	public static final ModelProperty<Model, SubSub> SUBSUB = SUBSUBLIST.defineElementAsChild();

	public static final ModelProperty<Model, String> SUBSUBID = SUBSUB.registerChild(sub -> sub.subSubId, (sub, value) -> {sub.subSubId = value;});
}
