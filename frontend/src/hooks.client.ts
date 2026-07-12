// import { SimpleSpanProcessor, WebTracerProvider } from '@opentelemetry/sdk-trace-web';
// import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';
// import { ZoneContextManager } from '@opentelemetry/context-zone';
// import { registerInstrumentations } from '@opentelemetry/instrumentation';
// import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-proto';
// import { resourceFromAttributes } from '@opentelemetry/resources';
// import { ATTR_SERVICE_NAME } from '@opentelemetry/semantic-conventions';

export async function init() {
	// const provider = new WebTracerProvider({
	// 	resource: resourceFromAttributes({
	// 		[ATTR_SERVICE_NAME]: 'sveltekit-frontend',
	// 	}),
	// 	spanProcessors: [new SimpleSpanProcessor(new OTLPTraceExporter())],
	// });
	//
	// provider.register({
	// 	contextManager: new ZoneContextManager(),
	// });
	//
	// registerInstrumentations({
	// 	instrumentations: [
	// 		getWebAutoInstrumentations({
	// 			'@opentelemetry/instrumentation-fetch': {
	// 				propagateTraceHeaderCorsUrls: [/localhost:\d+/],
	// 			},
	// 		}),
	// 	],
	// });
}
