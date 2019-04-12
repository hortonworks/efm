package com.cloudera.cem.efm.service

import com.cloudera.cem.efm.core.mapper.*
import com.cloudera.cem.efm.mapper.*
import org.modelmapper.Converter
import org.modelmapper.PropertyMap

class SpecUtil {

    static OptionalModelMapper buildOptionalModelMapper() {

        List<PropertyMap> propertyMaps = new ArrayList<>()
        propertyMaps.addAll(OperationConverterFactory.allMaps())
        propertyMaps.addAll(DeviceConverterFactory.allMaps())
        propertyMaps.addAll(AgentConverterFactory.allMaps())
        propertyMaps.addAll(EventConverterFactory.allMaps())

        List<Converter> converters = new ArrayList<>()
        converters.addAll(DateConverterFactory.allConverters())
        converters.addAll(HeartbeatConverterFactory.allConverters())
        converters.addAll(DeviceConverterFactory.allConverters())
        converters.addAll(AgentConverterFactory.allConverters())

        def modelMapper = new OptionalModelMapper(propertyMaps, converters)
        modelMapper.initialize()

        return modelMapper
    }

}
