<script lang="ts">
	import { PUBLIC_BACKEND_URL } from '$env/static/public';
	import { getMotd } from '$lib/backend.remote';
	import { tick } from 'svelte';

	let ws: WebSocket;

	$effect(() => {
		ws = new WebSocket(`${PUBLIC_BACKEND_URL}/ws`);
		ws.onmessage = (msg) => {
			messages.push(msg.data);
		};
	});

	let message = $state('');
	const messages: string[] = $state([]);

	const sendMessage = () => {
		ws.send(message);
		message = '';
	};

	let div: HTMLDivElement | undefined = $state();

	$effect.pre(() => {
		if (!div) return;

		messages.length;

		if (div.offsetHeight + div.scrollTop > div.scrollHeight - 20) {
			tick().then(() => {
				div!.scrollTo(0, div!.scrollHeight);
			});
		}

		div.scrollTo(0, div.scrollHeight);
	});
</script>

<div class="wrapper">
	<h3>Message of the Day: {await getMotd()}</h3>

	<div class="messages-container">
		<div class="chatbox" bind:this={div}>
			<div class="message">MESSAGE</div>
			{#each messages as msg}
				<div class="message">{msg}</div>
			{/each}
		</div>
		<form>
			<input
				class="message-input"
				type="text"
				placeholder="Type a message..."
				bind:value={message}
			/>
			<button type="submit" onclick={sendMessage}>Send message</button>
		</form>
	</div>
</div>

<style>
	.wrapper {
		display: flex;
		flex-direction: column;
		height: 100vh;
		width: 100vw;
	}

	.messages-container {
		display: flex;
		flex-direction: column;
		flex-grow: 1;
		min-height: 0;
	}

	.chatbox {
		background-color: gray;
		overflow-y: auto;
		flex-grow: 1;
		border: 2px solid black;
		display: flex;
		flex-direction: column;
		justify-content: flex-end;
	}

	.message {
		margin: 0px 5px 5px;
		border: 1px solid black;
		padding: 10px;
		color: white;
		background-color: gray;
	}
</style>
