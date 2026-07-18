import pino from 'pino';

const isProduction = process.env.NODE_ENV === 'production';
const transport = pino.transport({
	level: process.env.PINO_LOG_LEVEL?.toLowerCase() || 'debug',
	targets: [
		{
			target: 'pino-opentelemetry-transport',
		},
		isProduction
			? {
					target: '',
				}
			: {
					target: 'pino-pretty',
					options: { colorize: true },
				},
	],
});

export const logger = pino(transport);
