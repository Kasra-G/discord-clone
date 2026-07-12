import { createRequire } from 'module';
globalThis.require = createRequire(import.meta.url);

import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
// @ts-expect-error: import-in-the-middle lacks typescript types
import { register } from 'import-in-the-middle/register-hooks.mjs';
import { SimpleLogRecordProcessor } from '@opentelemetry/sdk-logs';
import { PinoInstrumentation } from '@opentelemetry/instrumentation-pino'; // Add this
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-grpc';
import { OTLPLogExporter } from '@opentelemetry/exporter-logs-otlp-grpc';

register();

const sdk = new NodeSDK({
	traceExporter: new OTLPTraceExporter(),
	metricReader: new PeriodicExportingMetricReader({
		exporter: new OTLPMetricExporter(),
	}),
	logRecordProcessor: new SimpleLogRecordProcessor({
		exporter: new OTLPLogExporter(),
	}),
	instrumentations: [
		getNodeAutoInstrumentations(),
		new PinoInstrumentation({
			logKeys: {
				traceId: 'trace_id',
				spanId: 'span_id',
				traceFlags: 'trace_flags',
			},
		}),
	],
});

sdk.start();
