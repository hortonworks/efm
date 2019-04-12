/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.mapper;


import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An extension of {@link org.modelmapper.ModelMapper} that adds convenience methods
 * for mapping objects wrapped in an Optional.
 *
 * Example usage in a Spring Boot project:
 *
 * <pre>{@code
 *
 * @Autowired
 * private OptionalModelMapper modelMapper;
 *
 * public Optional<MyDTO> getMyDTO(String id) {
 *     Optional<MyEntity> entityOptional = myRepository.findById(id);
 *     return modelMapper.mapOptional(entityOptional, MyDTO.class);
 * }
 *
 * }</pre>
 *
 * Aside from the added convenience methods, existing {@link ModelMapper} functionality works
 * normally and this class can be used anywhere a {@link ModelMapper} is needed.
 *
 * To customize this Spring Bean with {@link Converter}s and {@link PropertyMap}s, simply
 * define them and add component scanning annotations.
 */
@Component
public class OptionalModelMapper extends ModelMapper {

    private List<Converter> converters;
    private List<PropertyMap> propertyMaps;

    @Autowired
    public OptionalModelMapper(Optional<List<PropertyMap>> propertyMaps, Optional<List<Converter>> converters) {
        this(propertyMaps.orElse(Collections.EMPTY_LIST), converters.orElse(Collections.EMPTY_LIST));
    }

    public OptionalModelMapper() {
        this(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    public OptionalModelMapper(List<PropertyMap> propertyMaps, List<Converter> converters) {
        super();
        this.propertyMaps = propertyMaps != null ? propertyMaps : Collections.EMPTY_LIST;
        this.converters = converters != null ? converters : Collections.EMPTY_LIST;
    }

    @PostConstruct
    public void initialize() {
        propertyMaps.forEach(propertyMap -> this.addMappings(propertyMap));
        converters.forEach(converter -> this.addConverter(converter));
    }

    /**
     * Conditionally maps a source object in an Optional container to an instance of destinationType.
     * If the source Optional is empty, an empty optional is returned.
     * Otherwise, the result of source.get() is mapped to destinationType and returned in an Optional.
     *
     * @param source An optional containing the object to map
     * @param destinationType the type of instance to create
     * @return an {@code Optional<D>} containing and instance of the destinationType D if source isPresent,
     *         otherwise an empty Optional.
     */
    public <D> Optional<D> mapOptional(Optional source, Class<D> destinationType) {
        if (source == null || !source.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(map(source.get(), destinationType));
    }

    /**
     * Conditionally maps a source object in an Optional container to an instance of destinationType.
     * If the source Optional is empty, an empty optional is returned.
     * Otherwise, the result of source.get() is mapped to destinationType and returned in an Optional.
     *
     * @param source An optional containing the object to map
     * @param destinationType the type of instance to create
     * @param typeMapName the name of an existing TypeMap to use to perform the mapping.
     * @return an {@code Optional<D>} containing and instance of the destinationType D if source isPresent,
     *         otherwise an empty Optional.
     */
    public <D> Optional<D> mapOptional(Optional source, Class<D> destinationType, String typeMapName) {
        if (source == null || !source.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(map(source.get(), destinationType, typeMapName));
    }

    /**
     * Conditionally maps a source object in an Optional container to an instance of destinationType.
     * If the source Optional is empty, an empty optional is returned.
     * Otherwise, the result of source.get() is mapped to destinationType and returned in an Optional.
     *
     * @param source An optional containing the object to map
     * @param destinationType the type of instance to create
     * @return an {@code Optional<D>} containing and instance of the destinationType D if source isPresent,
     *         otherwise an empty Optional.
     */
    public <D> Optional<D> mapOptional(Optional source, Type destinationType) {
        if (source == null || !source.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(map(source.get(), destinationType));
    }

    /**
     * Conditionally maps a source object in an Optional container to an instance of destinationType.
     * If the source Optional is empty, an empty optional is returned.
     * Otherwise, the result of source.get() is mapped to destinationType and returned in an Optional.
     *
     * @param source An optional containing the object to map
     * @param destinationType the type of instance to create
     * @param typeMapName the name of an existing TypeMap to use to perform the mapping.
     * @return an {@code Optional<D>} containing and instance of the destinationType D if source isPresent,
     *         otherwise an empty Optional.
     */
    public <D> Optional<D> mapOptional(Optional source, Type destinationType, String typeMapName) {
        if (source == null || !source.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(map(source.get(), destinationType, typeMapName));
    }

}
