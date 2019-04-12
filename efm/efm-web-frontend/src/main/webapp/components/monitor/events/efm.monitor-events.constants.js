var TIME_RANGES = {
    ALL: 'ALL',
    LAST_HOUR: 'LAST_HOUR',
    LAST_4_HOURS: 'LAST_4_HOURS',
    LAST_24_HOURS: 'LAST_24_HOURS',
    LAST_7_DAYS: 'LAST_7_DAYS'
};

var TIME_RANGE_VALUES = [
    { label: 'All', value: TIME_RANGES.ALL },
    { label: 'Last Hour', value: TIME_RANGES.LAST_HOUR },
    { label: 'Last 4 Hours', value: TIME_RANGES.LAST_4_HOURS },
    { label: 'Last 24 Hours', value: TIME_RANGES.LAST_24_HOURS },
    { label: 'Last 7 Days', value: TIME_RANGES.LAST_7_DAYS }
];

var EVENT_COLUMNS = [
    {
        name: 'level',
        label: 'Severity',
        sortable: true,
        searchable: true,
        tooltip: 'Severity',
        width: '5%'
    },
    {
        name: 'eventType',
        label: 'Event Type',
        sortable: true,
        searchable: true,
        tooltip: 'Event Type',
        width: '8%'
    },
    {
        name: 'message',
        label: 'Message',
        sortable: true,
        searchable: true,
        tooltip: 'Message',
        width: '25%'
    },
    {
        name: 'eventSourceId',
        label: 'Event Source',
        sortable: true,
        searchable: true,
        tooltip: 'Event Source',
        link: 'source',
        width: '20%'
    },
    {
        name: 'eventSourceType',
        label: 'Source Type',
        sortable: true,
        searchable: true,
        tooltip: 'Agent or Server',
        width: '7%'
    },
    {
        name: 'agentClass',
        label: 'Class',
        sortable: true,
        searchable: true,
        tooltip: 'Agent Class',
        link: 'agentClass',
        width: '6%'
    },
    {
        name: 'created',
        label: 'Date/Time',
        sortable: true,
        searchable: false,
        tooltip: 'Date/Time',
        width: '12%'
    }
];

var ROWS = [
    { label: '20', value: '20' },
    { label: '50', value: '50' },
    { label: '100', value: '100' }
];

var AUTO_REFRESH_SECONDS = 30;

module.exports = {
    TIME_RANGES: TIME_RANGES,
    TIME_RANGE_VALUES: TIME_RANGE_VALUES,
    EVENT_COLUMNS: EVENT_COLUMNS,
    ROWS: ROWS,
    AUTO_REFRESH_SECONDS: AUTO_REFRESH_SECONDS
};
