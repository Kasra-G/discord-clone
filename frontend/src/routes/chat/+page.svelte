<script lang="ts">
	import { getMotd } from '$lib/backend.remote';
	import { messageService } from '$lib/websocket.svelte';
	import { tick } from 'svelte';

	interface UserMessage {
		readonly message: string;
		readonly username: string;
		readonly receivedAt: string;
	}

	let message = $state('');
	const messages: UserMessage[] = $derived(messageService.messages.map((elem) => JSON.parse(elem)));

	const sendMessage = () => {
		messageService.sendMessage(message);
		message = '';
	};

	let div: HTMLDivElement | undefined = $state();

	let isChatOverflowing = $derived.by(() => {
		if (!div) return false;
		messages.length;

		return div.scrollHeight > div.clientHeight;
	});

	// scroll on first overflow
	$effect.pre(() => {
		if (!div) return;
		if (isChatOverflowing) {
			tick().then(() => {
				div!.scrollTo(0, div!.scrollHeight);
			});
		}
	});

	// scroll when bottom
	$effect.pre(() => {
		if (!div) return;

		messages.length;

		if (div.scrollHeight - div.scrollTop - div.clientHeight < 1) {
			tick().then(() => {
				div!.scrollTo(0, div!.scrollHeight);
			});
		}
	});
</script>

<div class="wrapper">
	<h3>Message of the Day: {await getMotd()}</h3>
	<h3>Server status: {messageService.status}</h3>

	<div class="messages-container">
		<div class="chatbox-outer" bind:this={div}>
			<div class="chatbox-inner">
				{#each messages.toReversed() as msg}
					<div class="message">
						User: {msg.username} time: {msg.receivedAt} Message: {msg.message}
					</div>
				{/each}
			</div>
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

	.chatbox-outer {
		overflow-y: auto;
		flex-grow: 1;
		border: 2px solid black;
		background-color: gray;
	}

	.chatbox-inner {
		min-height: 100%;
		display: flex;
		flex-direction: column-reverse;
	}

	.message {
		margin: 0px 5px 5px;
		border: 1px solid black;
		padding: 10px;
		color: white;
		background-color: gray;
	}
</style>
