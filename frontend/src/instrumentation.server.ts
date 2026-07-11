import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
// @ts-expect-error - import-in-the-middle has no official types
import { register } from 'import-in-the-middle/register-hooks.mjs';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
import { credentials } from '@grpc/grpc-js';

register();

const sdk = new NodeSDK({
	serviceName: 'frontend',
	traceExporter: new OTLPTraceExporter({
		credentials: credentials.createInsecure(),
	}),
	instrumentations: [getNodeAutoInstrumentations()],
});

sdk.start();
