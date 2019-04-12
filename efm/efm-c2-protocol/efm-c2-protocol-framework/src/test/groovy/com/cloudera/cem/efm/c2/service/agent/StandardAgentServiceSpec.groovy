/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *      LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *      FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *      DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *      TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *      UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.agent

import com.cloudera.cem.efm.db.entity.AgentClassEntity
import com.cloudera.cem.efm.db.entity.AgentEntity
import com.cloudera.cem.efm.db.entity.AgentManifestEntity
import com.cloudera.cem.efm.db.repository.AgentClassRepository
import com.cloudera.cem.efm.db.repository.AgentManifestRepository
import com.cloudera.cem.efm.db.repository.AgentRepository
import com.cloudera.cem.efm.exception.ResourceNotFoundException
import com.cloudera.cem.efm.mapper.OptionalModelMapper
import com.cloudera.cem.efm.model.Agent
import com.cloudera.cem.efm.model.AgentClass
import com.cloudera.cem.efm.model.AgentManifest
import com.cloudera.cem.efm.service.SpecUtil
import spock.lang.Specification

import javax.validation.ConstraintViolationException
import javax.validation.Validation
import javax.validation.Validator

class StandardAgentServiceSpec extends Specification {

    static OptionalModelMapper modelMapper
    static Validator validator
    AgentManifestRepository agentManifestRepository
    AgentClassRepository agentClassRepository
    AgentRepository agentRepository

    AgentService agentService

    def setupSpec() {
        validator = Validation.buildDefaultValidatorFactory().getValidator()
        modelMapper = SpecUtil.buildOptionalModelMapper()
    }

    def setup() {
        agentManifestRepository = Mock(AgentManifestRepository)
        agentClassRepository = Mock(AgentClassRepository)
        agentRepository = Mock(AgentRepository)

        agentService = new StandardAgentService(
                agentManifestRepository,
                agentClassRepository,
                agentRepository,
                validator,
                modelMapper)
    }

    //**********************************
    //***  Agent Class CRUD methods  ***
    //**********************************

    def "create agent class"() {

        setup:
        agentManifestRepository.existsById("missingManifest") >> false
        agentManifestRepository.existsById("existingManifest") >> true
        agentManifestRepository.existsById("anotherExistingManifest") >> true
        agentClassRepository.existsById("myClass") >> false
        agentClassRepository.existsById("myDupeClass") >> true
        agentClassRepository.save(_ as AgentClassEntity) >> { AgentClassEntity e -> return e }

        when: "arg is null"
        agentService.createAgentClass(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "class name is null"
        agentService.createAgentClass(new AgentClass())

        then: "exception is thrown"
        thrown ConstraintViolationException


        when: "class with same name already exists"
        agentService.createAgentClass(new AgentClass([name: "myDupeClass", description: "myDescription2"]))

        then: "exception is thrown"
        thrown IllegalStateException


        when: "class with missing manifests"
        agentService.createAgentClass(new AgentClass([
                name: "myDupeClass",
                description: "myDescription",
                agentManifests: new HashSet<String>(["missingManifest"])
        ]))

        then: "exception is thrown"
        thrown IllegalStateException


        when: "valid class is created with >1 manifests"
        agentService.createAgentClass(new AgentClass([
                name: "myClass",
                description: "myDescription",
                agentManifests: new HashSet<String>(["existingManifest", "anotherExistingManifest"])
        ]))

        then: "exception is thrown"
        thrown ConstraintViolationException


        when: "valid class is created without manifests"
        def createdClass1 = agentService.createAgentClass(new AgentClass([
                name: "myClass",
                description: "myDescription"
        ]))

        then: "created class is returned"
        with(createdClass1) {
            name == "myClass"
            description == "myDescription"
            agentManifests == null
        }


        when: "valid class is created with manifests"
        def createdClass2 = agentService.createAgentClass(new AgentClass([
                name: "myClass",
                description: "myDescription",
                agentManifests: new HashSet<String>(["existingManifest"])
        ]))

        then: "created class is returned"
        with(createdClass2) {
            name == "myClass"
            description == "myDescription"
            agentManifests.size() == 1
            agentManifests.contains("existingManifest")
        }

    }

    def "get agent classes"() {

        setup:
        agentClassRepository.findAll() >> [new AgentClassEntity([id: "class1"]),
                                           new AgentClassEntity([id: "class2"]),
                                           new AgentClassEntity([id: "class3"])]

        when:
        def classes = agentService.getAgentClasses()

        then:
        classes != null
        classes.size() == 3
        with(classes.get(0)) {
            name: "class1"
        }

    }

    def "get agent class"() {

        setup:
        agentClassRepository.existsById("myMissingClass") >> false
        agentClassRepository.findById("myMissingClass") >> Optional.empty()
        agentClassRepository.existsById("myClass") >> true
        agentClassRepository.findById("myClass") >> Optional.of(new AgentClassEntity([id: "myClass"]))


        when: "class name arg is null"
        agentService.getAgentClass(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "class does not exist"
        def ac1 = agentService.getAgentClass("myMissingClass")

        then: "empty optional is returned"
        !ac1.isPresent()


        when: "class does exist"
        def ac2 = agentService.getAgentClass("myClass")

        then: "class is returned"
        ac2.isPresent()
        with(ac2.get()) {
            name == "myClass"
        }

    }

    def "update agent class"() {

        setup:
        agentManifestRepository.existsById("missingManifest") >> false
        agentManifestRepository.existsById("existingManifest") >> true
        agentClassRepository.existsById("myMissingClass") >> false
        agentClassRepository.findById("myMissingClass") >> Optional.empty()
        agentClassRepository.existsById("myClass") >> true
        agentClassRepository.findById("myClass") >> Optional.of(new AgentClassEntity(id: "myClass"))
        agentClassRepository.save(_ as AgentClassEntity) >> { AgentClassEntity e -> return e }


        when: "arg is null"
        agentService.updateAgentClass(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "class name is null"
        agentService.updateAgentClass(new AgentClass())

        then: "exception is thrown"
        thrown ConstraintViolationException


        when: "class does not exist"
        agentService.updateAgentClass(new AgentClass([name: "myMissingClass", description: "new description"]))

        then:
        thrown ResourceNotFoundException


        when: "class exists, but updates are invalid due to missing manifests"
        agentService.updateAgentClass(new AgentClass([
                name: "myClass",
                description: "new description",
                agentManifests: new HashSet<String>(["missingManifest"])
        ]))

        then: "exception is thrown"
        thrown IllegalStateException


        when: "class exists, updates are valid"
        def updatedClass = agentService.updateAgentClass(new AgentClass([
                name: "myClass",
                description: "new description",
                agentManifests: new HashSet<String>(["existingManifest"])
        ]))

        then:
        with(updatedClass) {
            name == "myClass"
            description == "new description"
            agentManifests.size() == 1
            agentManifests.contains("existingManifest")
        }

    }

    def "delete agent class"() {

        setup:
        agentClassRepository.existsById("myMissingClass") >> false
        agentClassRepository.findById("myMissingClass") >> Optional.empty()
        agentClassRepository.existsById("myClass") >> true
        agentClassRepository.findById("myClass") >> Optional.of(new AgentClassEntity([id: "myClass"]))
        agentClassRepository.save(_ as AgentClassEntity) >> { AgentClassEntity e -> return e }


        when: "arg is null"
        agentService.deleteAgentClass(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "class does not exist"
        agentService.deleteAgentClass("myMissingClass")

        then:
        thrown ResourceNotFoundException


        when: "class exists"
        def deletedClass = agentService.deleteAgentClass("myClass")

        then:
        with(deletedClass) {
            name == "myClass"
        }

    }


    //*************************************
    //***  Agent Manifest CRUD methods  ***
    //*************************************

    def "create agent manifest"() {

        setup:
        agentManifestRepository.existsById("manifest1") >> false
        agentManifestRepository.existsById("manifest2") >> true
        agentManifestRepository.save(_ as AgentManifestEntity) >> { AgentManifestEntity e -> return e }

        when: "arg is null"
        agentService.createAgentManifest(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "valid manifest is created without client-set id"
        def created = agentService.createAgentManifest(new AgentManifest([agentType: "java"]))

        then: "manifest is created and assigned an id"
        with(created) {
            identifier != null
        }


        when: "valid manifest is created with client-set id"
        def created2 = agentService.createAgentManifest(new AgentManifest([identifier: "manifest1", agentType: "java"]))

        then: "manifest is created with client-set id"
        with(created2) {
            identifier == "manifest1"
            agentType == "java"
        }


        when: "manifest is created with client-set id that already exists"
        agentService.createAgentManifest(new AgentManifest([identifier: "manifest2", agentType: "java"]))

        then: "exception is thrown"
        thrown IllegalStateException

    }

    def "get agent manifests"() {

        setup:
        agentManifestRepository.findAll() >> [new AgentManifestEntity([id: "manifest1"]),
                                              new AgentManifestEntity([id: "manifest2"])]

        when:
        def manifests = agentService.getAgentManifests()

        then:
        manifests != null
        manifests.size() == 2
        with(manifests.get(0)) {
            identifier: "manifest1"
        }

    }

    def "get agent manifests by class name"() {

        setup:

        final AgentClassEntity myClass = new AgentClassEntity([id: "myClass", agentManifests: ["manifest1", "manifest2"].toSet()])
        agentClassRepository.findById("myClass") >> Optional.of(myClass)
        agentManifestRepository.findAllById(_) >> [new AgentManifestEntity([id: "manifest1"]),
                                                   new AgentManifestEntity([id: "manifest2"])]

        when:
        def manifests = agentService.getAgentManifests("myClass")

        then:
        manifests != null
        manifests.size() == 2
        manifests.get(0).getIdentifier() == "manifest1"
        manifests.get(1).getIdentifier() == "manifest2"

    }

    def "get agent manifest"() {

        setup:
        agentManifestRepository.findById("manifest1") >> Optional.empty()
        agentManifestRepository.findById("manifest2") >> Optional.of(new AgentManifestEntity([id: "manifest2"]))


        when: "id arg is null"
        agentService.getAgentManifest(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "manifest does not exist"
        def manifest1 = agentService.getAgentManifest("manifest1")

        then: "empty optional is returned"
        !manifest1.isPresent()


        when: "manifest exists"
        def manifest2 = agentService.getAgentManifest("manifest2")

        then: "manifest is returned"
        manifest2.isPresent()
        with(manifest2.get()) {
            identifier == "manifest2"
        }

    }

    def "delete agent manifest"() {

        setup:
        agentManifestRepository.findById("manifest1") >> Optional.empty()
        agentManifestRepository.findById("manifest2") >> Optional.of(new AgentManifestEntity([id: "manifest2"]))


        when: "id arg is null"
        agentService.deleteAgentManifest(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "manifest does not exist"
        agentService.deleteAgentManifest("manifest1")

        then: "exception is thrown"
        thrown ResourceNotFoundException


        when: "manifest exists"
        def deleted = agentService.deleteAgentManifest("manifest2")

        then: "deleted manifest is returned"
        with(deleted) {
            identifier == "manifest2"
        }

    }


    //****************************
    //***  Agent CRUD methods  ***
    //****************************

    def "create agent"() {

        setup:
        agentRepository.existsById("agent1") >> false
        agentRepository.existsById("agent2") >> true
        agentRepository.findById("agent2") >> Optional.of(new AgentEntity([id: "agent2"]))
        agentRepository.save(_ as AgentEntity) >> {AgentEntity e -> return e}

        when: "arg is null"
        agentService.createAgent(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "valid agent is created"
        def created = agentService.createAgent(new Agent([identifier: "agent1"]))

        then: "created agent is returned"
        with(created) {
            identifier == "agent1"
        }


        when: "agent is created with id that already exists"
        agentService.createAgent(new Agent([identifier: "agent2"]))

        then: "exception is thrown"
        thrown IllegalStateException

    }

    def "get agents"() {

        setup:
        agentRepository.findAll() >> [new AgentEntity([id: "agent1"]),
                                      new AgentEntity([id: "agent2"]),
                                      new AgentEntity([id: "agent3"])]

        when:
        def agents = agentService.getAgents()

        then:
        agents.size() == 3
        agents.get(0).identifier == "agent1"
        agents.get(1).identifier == "agent2"
        agents.get(2).identifier == "agent3"

    }

    def "get agents by class name"() {

        setup:
        agentRepository.findAllByAgentClass(_) >> [new AgentEntity([id: "agent1"]),
                                                   new AgentEntity([id: "agent2"])]

        when:
        def agents = agentService.getAgents("myClass")

        then:
        agents.size() == 2
        agents.get(0).identifier == "agent1"
        agents.get(1).identifier == "agent2"

    }

    def "get agent"() {

        setup:
        agentRepository.existsById("agent1") >> false
        agentRepository.existsById("agent2") >> true
        agentRepository.findById("agent2") >> Optional.of(new AgentEntity([id: "agent2"]))

        when: "id arg is null"
        agentService.getAgent(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "agent does not exist"
        def agent1 = agentService.getAgent("agent1")

        then: "empty optional is returned"
        !agent1.isPresent()


        when: "agent exists"
        def agent2 = agentService.getAgent("agent2")

        then: "agent is returned"
        agent2.isPresent()
        agent2.get().identifier == "agent2"

    }

    def "update agent"() {

        setup:
        agentRepository.existsById("agent1") >> false
        agentRepository.findById("agent1") >> Optional.empty()
        agentRepository.existsById("agent2") >> true
        agentRepository.findById("agent2") >> Optional.of(new AgentEntity([id: "agent2"]))
        agentRepository.save(_ as AgentEntity) >> {AgentEntity e -> return e}

        when: "arg is null"
        agentService.updateAgent(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "agent does not exist"
        agentService.updateAgent(new Agent([identifier: "agent1", name: "a better agent"]))

        then: "exception is thrown"
        thrown ResourceNotFoundException


        when: "agent exists, update is valid"
        def updated = agentService.updateAgent(new Agent([identifier: "agent2", name: "a better agent"]))

        then: "updated agent is returned"
        with(updated) {
            identifier == "agent2"
            name == "a better agent"
        }

    }

    def "delete agent"() {

        setup:
        agentRepository.existsById("agent1") >> false
        agentRepository.findById("agent1") >> Optional.empty()
        agentRepository.existsById("agent2") >> true
        agentRepository.findById("agent2") >> Optional.of(new AgentEntity([id: "agent2"]))
        agentRepository.save(_ as AgentEntity) >> {AgentEntity e -> return e}


        when: "arg is null"
        agentService.deleteAgent(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "agent does not exist"
        agentService.deleteAgent("agent1")

        then:
        thrown ResourceNotFoundException


        when: "agent exists"
        def deleted = agentService.deleteAgent("agent2")

        then:
        with(deleted) {
            identifier == "agent2"
        }

    }

}
