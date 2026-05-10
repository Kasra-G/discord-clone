<script lang="ts">
	import { PUBLIC_BACKEND_URL } from '$env/static/public';

	let { data } = $props();
	let ws: WebSocket;
	let serverResponse = $state('Nothing!');
</script>

<h1>{data.serverMessage}</h1>
<button
	onclick={() => {
		ws = new WebSocket(`${PUBLIC_BACKEND_URL}/ws`);
		ws.onmessage = (msg) => {
			serverResponse = msg.data;
		};
	}}>Open connection</button
>
<button onclick={() => ws.close()}>Close connection</button>
<button onclick={() => ws.send(`omg`)}>Send random message</button>
<h1>Server response: {serverResponse}</h1>
