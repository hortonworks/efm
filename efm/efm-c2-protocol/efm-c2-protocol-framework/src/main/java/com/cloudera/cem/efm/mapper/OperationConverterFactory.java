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

import com.cloudera.cem.efm.db.entity.OperationEntity;
import com.cloudera.cem.efm.model.C2Operation;
import com.cloudera.cem.efm.model.Operation;
import org.modelmapper.Condition;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class OperationConverterFactory {

    @Bean
    public static PropertyMap<OperationEntity, C2Operation> entityToC2OperationMap() {
        return new PropertyMap<OperationEntity, C2Operation>() {
            @Override
            protected void configure() {
                map().setIdentifier(source.getId());
            }
        };
    }

    @Bean
    public static PropertyMap<OperationEntity, Operation> entityToOperationMap() {
        return new PropertyMap<OperationEntity, Operation>() {
            @Override
            protected void configure() {
                map().setIdentifier(source.getId());

                // if the entity set of agent dependency operation ids is empty, do not set the destination
                // so that this field is omitted when serializing to json (we serialize not-null by default)
                when((Condition<Set, Set>) context ->
                        context.getSource() != null && !context.getSource().isEmpty())
                        .map().setDependencies(source.getDependencies());

                // Note: Date -> Long conversions configured by DateToLongConverter
            }
        };
    }

    @Bean
    public static PropertyMap<Operation, OperationEntity> operationToEntityMap() {
        return new PropertyMap<Operation, OperationEntity>() {
            @Override
            protected void configure() {
                map().setId(source.getIdentifier());

                // Note: Long -> Date conversions configured by LongToDateConverter
            }
        };
    }

    public static List<PropertyMap<?, ?>> allMaps() {
        return Arrays.asList(
                entityToC2OperationMap(),
                entityToOperationMap(),
                operationToEntityMap()
        );
    }

}