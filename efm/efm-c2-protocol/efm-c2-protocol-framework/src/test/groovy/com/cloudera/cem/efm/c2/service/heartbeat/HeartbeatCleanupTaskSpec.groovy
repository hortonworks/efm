package com.cloudera.cem.efm.service.heartbeat

import com.cloudera.cem.efm.db.entity.HeartbeatEntity
import com.cloudera.cem.efm.db.projection.IdAndNumber
import com.cloudera.cem.efm.db.projection.IdAndTimestamp
import com.cloudera.cem.efm.db.repository.HeartbeatRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

class HeartbeatCleanupTaskSpec extends Specification {

    // Relative times for us to use...
    private static Instant zero = Instant.now().minus(Duration.ofDays(7))

    private static HeartbeatEntity hb1  // agent-1, zero + 1 day,  null content
    private static HeartbeatEntity hb2  // agent-2, zero + 2 days, null content
    private static HeartbeatEntity hb3  // null,    zero + 3 days, null content
    private static HeartbeatEntity hb4  // agent-1, zero + 4 days, nonnull content
    private static HeartbeatEntity hb5  // null,    zero + 5 days, nonnull content
    private static HeartbeatEntity hb6  // agent-1, zero + 6 days, nonnull content

    private static List<HeartbeatEntity> heartbeats

    private static final Comparator<HeartbeatEntity> CREATED_DESC = new Comparator<HeartbeatEntity>() {
        @Override
        int compare(HeartbeatEntity o1, HeartbeatEntity o2) {
            return o1.getCreated().before(o2.getCreated()) ? -1 : o1.getCreated().equals(o2.getCreated()) ? 0 : 1
        }
    }.reversed()

    private static final Comparator<IdAndNumber> COUNT_DESC = new Comparator<IdAndNumber>() {
        @Override
        int compare(IdAndNumber o1, IdAndNumber o2) {
            return o1.getNumber() < o2.getNumber() ? -1 : o1.getNumber() == o2.getNumber() ? 0 : 1
        }
    }.reversed()

    private HeartbeatRepository repository

    def setupSpec() {
        hb1 = new HeartbeatEntity([id: "heartbeat-1", agentId: "agent-1", created: Date.from(zero.plus(Duration.ofDays(1))), content: null])
        hb2 = new HeartbeatEntity([id: "heartbeat-2", agentId: "agent-2", created: Date.from(zero.plus(Duration.ofDays(2))), content: null])
        hb3 = new HeartbeatEntity([id: "heartbeat-3", agentId: null,      created: Date.from(zero.plus(Duration.ofDays(3))), content: null])
        hb4 = new HeartbeatEntity([id: "heartbeat-4", agentId: "agent-1", created: Date.from(zero.plus(Duration.ofDays(4))), content: "c4"])
        hb5 = new HeartbeatEntity([id: "heartbeat-5", agentId: null,      created: Date.from(zero.plus(Duration.ofDays(5))), content: "c5"])
        hb6 = new HeartbeatEntity([id: "heartbeat-6", agentId: "agent-1", created: Date.from(zero.plus(Duration.ofDays(6))), content: "c6"])

        heartbeats = [hb1, hb2, hb3, hb4, hb5, hb6]
    }

    def setup() {
        repository = Mock(HeartbeatRepository)
        repository.findAll() >> heartbeats
        repository.findById(_ as String) >> { String id -> heartbeats.stream().filter({it.getId() == id}).findFirst() }
        repository.findByAgentId(_ as String) >> { String agentId ->
            heartbeats.stream().filter({
                Objects.equals(it.getAgentId(), agentId)
            }).collect(Collectors.toList())
        }
        repository.findHeartbeatTimestampsByAgentId(_ as String, _ as Pageable) >> { String agentId, Pageable p ->
            heartbeats.stream()
                    .sorted(CREATED_DESC)
                    .filter({ it.getAgentId() != null && it.getAgentId() == agentId })
                    .map({ new IdAndTimestamp(it.getId(), it.getCreated()) })
                    .limit(p.getPageSize())
                    .collect(Collectors.toList())
        }
        repository.findHeartbeatTimestampsByAgentIdIsNull(_ as Pageable) >> { Pageable p ->
            heartbeats.stream()
                    .sorted(CREATED_DESC)
                    .filter({ it.getAgentId() == null })
                    .map({ new IdAndTimestamp(it.getId(), it.getCreated()) })
                    .limit(p.getPageSize())
                    .collect(Collectors.toList())
        }
        repository.findHeartbeatContentTimestampsByAgentId(_ as String, _ as Pageable) >> { String agentId, Pageable p ->
            heartbeats.stream()
                    .sorted(CREATED_DESC)
                    .filter({ it.getAgentId() != null && it.getAgentId() == agentId && it.getContent() != null })
                    .map({ new IdAndTimestamp(it.getId(), it.getCreated()) })
                    .limit(p.getPageSize())
                    .collect(Collectors.toList())
        }
        repository.findHeartbeatContentTimestampsByAgentIdIsNull(_ as Pageable) >> { Pageable p ->
            heartbeats.stream()
                    .sorted(CREATED_DESC)
                    .filter({ it.getAgentId() == null && it.getContent() != null })
                    .map({ new IdAndTimestamp(it.getId(), it.getCreated()) })
                    .limit(p.getPageSize())
                    .collect(Collectors.toList())
        }
        repository.findHeartbeatCountsByAgentId() >> {
            List<IdAndNumber> result = new ArrayList<>()
            Map<String, Long> countsByAgentId = new HashMap<>()
            heartbeats.forEach({
                Long oldCount = countsByAgentId.getOrDefault(it.getAgentId(), Long.valueOf(0))
                countsByAgentId.put(it.getAgentId(), oldCount + 1)
            })
            countsByAgentId.forEach({ String agentId, Long count -> result.add(new IdAndNumber(agentId, count))})
            result.sort(COUNT_DESC)
            return result
        }
        repository.findHeartbeatContentCountsByAgentId() >> {
            List<IdAndNumber> result = new ArrayList<>()
            Map<String, Long> countsByAgentId = new HashMap<>()
            heartbeats.forEach({
                if (it.getContent() != null) {
                    Long oldCount = countsByAgentId.getOrDefault(it.getAgentId(), Long.valueOf(0))
                    countsByAgentId.put(it.getAgentId(), oldCount + 1)
                }
            })
            countsByAgentId.forEach({ String agentId, Long count -> result.add(new IdAndNumber(agentId, count))})
            result.sort(COUNT_DESC)
            return result
        }

    }

    def selfCheckRepositoryMock() {

        expect:
        repository.findAll().size() == heartbeats.size()
        repository.findById("heartbeat-1").get() == hb1
        repository.findByAgentId("agent-1").toList() == [hb1, hb4, hb6]
        repository.findHeartbeatTimestampsByAgentId("agent-1", PageRequest.of(0, 10)) ==
                idAndTimestampsFrom(hb6, hb4, hb1)
        repository.findHeartbeatTimestampsByAgentIdIsNull(PageRequest.of(0, 10)) ==
                idAndTimestampsFrom(hb5, hb3)
        repository.findHeartbeatContentTimestampsByAgentId("agent-1", PageRequest.of(0, 10)) ==
                idAndTimestampsFrom(hb6, hb4)
        repository.findHeartbeatContentTimestampsByAgentIdIsNull(PageRequest.of(0, 10)) ==
                idAndTimestampsFrom(hb5)
        repository.findHeartbeatCountsByAgentId().toList() ==
                [ new IdAndNumber("agent-1", 3L),
                  new IdAndNumber(null, 2L),
                  new IdAndNumber("agent-2", 1L) ]

    }

    // Helpers

    private static IdAndTimestamp idAndTimestampFrom(HeartbeatEntity hb) {
        return new IdAndTimestamp(hb.getId(), hb.getCreated())
    }

    private static List<IdAndTimestamp> idAndTimestampsFrom(HeartbeatEntity... hb) {
        hb.toList().stream().map({ idAndTimestampFrom(it)}).collect(Collectors.toList())
    }


    // Specifications

    def nullRetention() {
        given: "a task with null retention properties"
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, new HeartbeatProperties())

        when: "the task is run"
        task.run()

        then: "the task does not interact with the repository"
        0 * repository._  // no interactions with repository
    }

    def metadataMaxAgeToKeep() {
        setup:
        final HeartbeatProperties properties = new HeartbeatProperties()
        properties.setMetadata(new HeartbeatProperties.Retention())
        properties.getMetadata().setMaxAgeToKeep(Duration.ofDays(3))
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, properties)

        when:
        task.run()

        then:
        1 * repository.deleteByCreatedBefore(_)
    }

    def metadataMaxCountToKeep() {
        setup:
        final HeartbeatProperties properties = new HeartbeatProperties()
        properties.setMetadata(new HeartbeatProperties.Retention())
        properties.getMetadata().setMaxCountToKeep(1)
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, properties)

        when:
        task.run()

        then:
        1 * repository.deleteByAgentIdEqualsAndCreatedBefore("agent-1", hb6.getCreated())
        1 * repository.deleteByAgentIdIsNullAndCreatedBefore(hb5.getCreated())
    }

    def metadataMaxCountToKeepZero() {
        setup:
        final HeartbeatProperties properties = new HeartbeatProperties()
        properties.setMetadata(new HeartbeatProperties.Retention())
        properties.getMetadata().setMaxCountToKeep(0)
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, properties)

        when:
        task.run()

        then:
        1 * repository.deleteAll()
    }

    def contentMaxAgeToKeep() {
        setup:
        final HeartbeatProperties properties = new HeartbeatProperties()
        properties.setContent(new HeartbeatProperties.Retention())
        properties.getContent().setMaxAgeToKeep(Duration.ofDays(3))
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, properties)

        when:
        task.run()

        then:
        1 * repository.deleteContentByCreatedBefore(_)
    }

    def contentMaxCountToKeep() {
        setup:
        final HeartbeatProperties properties = new HeartbeatProperties()
        properties.setContent(new HeartbeatProperties.Retention())
        properties.getContent().setMaxCountToKeep(1)
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, properties)

        when:
        task.run()

        then:
        1 * repository.deleteContentByAgentIdEqualsAndCreatedBefore("agent-1", hb6.getCreated())
    }

    def contentMaxCountToKeepZero() {
        setup:
        final HeartbeatProperties properties = new HeartbeatProperties()
        properties.setContent(new HeartbeatProperties.Retention())
        properties.getContent().setMaxCountToKeep(0)
        HeartbeatCleanupTask task = new HeartbeatCleanupTask(repository, properties)

        when:
        task.run()

        then:
        1 * repository.deleteAllContent()
    }

}
