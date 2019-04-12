var MOCK_AGENTS = require('mock/agents.js');
var MOCK_CLASSES = [];

// TODO: server needs to add href to agent objects to facilitate links
MOCK_AGENTS.forEach(function (agent) {
    if (MOCK_CLASSES.length === 0) {
        agent.link = {
            href: '/class-detail/' + agent.id
        };
        MOCK_CLASSES.push(agent);
    } else {
        MOCK_CLASSES.forEach(function (clazz) {
            if (clazz.agent_class !== agent.agent_class) {
                agent.link = {
                    href: '/class-detail/' + agent.id
                };
                MOCK_CLASSES.push(agent);
            }
        });
    }
});

var MOCK_OVERVIEW = {
    classes: MOCK_CLASSES,
    servers: [
        {
            identity: 'c2Server',
            link: {
                href: '/server-detail/1234'
            }
        }
    ],
    events: [
        {
            identifier: '1234',
            identity: 'c2Server 1',
            status: 'FATAL',
            message: 'A message about the server.',
            link: {
                href: '/server-detail/1234'
            },
            updated: new Date()
        },
        {
            identifier: '6f6a1402-3298-4e99-8656-284c2f4ad42c',
            identity: 'Agent 1',
            status: 'Error',
            message: 'A message about the agent.',
            link: {
                href: '/instance-detail/6f6a1402-3298-4e99-8656-284c2f4ad42c'
            },
            updated: new Date()
        }
    ]
};

module.exports = MOCK_OVERVIEW;
