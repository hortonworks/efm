var MOCK_AGENTS = require('mock/agents.js');

var CLASS_DETAIL = {
    'identifier': '6f6a1402-3298-4e99-8656-284c2f4ad42c',
    'identity': 'techops',
    'registryIdentifier': '5p6a1402-3298-4e99-8656-284c2f4ad53d',
    'flow': {
        identifier: '1234',
        identity: 'tempor',
        versions: [
            {
                identifier: '1111',
                identity: '6068'
            },
            {
                identifier: '2222',
                identity: '6067'
            }
        ]
    },
    'classEvents': [
        {
            'timestamp': '2017-12-07T05:19:51 +05:00',
            'severity': 'trace',
            'type': 'flow_update',
            'message': 'There was an event for the agent'
        }
    ],
    'deploymentServers': [{
        identity: 'c2Server',
        link: {
            href: '/server-detail/1234'
        }
    }
    ],
    'agents': MOCK_AGENTS
};

module.exports = CLASS_DETAIL;
