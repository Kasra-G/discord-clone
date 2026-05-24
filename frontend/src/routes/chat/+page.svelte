<script lang="ts">
	import { getMotd } from '$lib/backend.remote';
	import { commandService } from '$lib/command-service.svelte';
	import { websocketService } from '$lib/websocket.svelte';
	import { tick } from 'svelte';

	let message = $state('');
	const messages = $derived(commandService.messages);

	const sendMessage = () => {
		commandService.newMessage(message);
		message = '';
	};

	let div: HTMLDivElement | undefined = $state();

	let isChatOverflowing = $derived.by(() => {
		if (!div) return false;

		// eslint-disable-next-line @typescript-eslint/no-unused-expressions
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

		// eslint-disable-next-line @typescript-eslint/no-unused-expressions
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
	<h3>Server status: {websocketService.status}</h3>

	<div class="messages-container">
		<div class="chatbox-outer" bind:this={div}>
			<div class="chatbox-inner">
				{#each messages.toReversed() as msg (msg.createdAt)}
					<div class="message">
						<div class="message-header">
							<div class="message-username">
								{msg.sentBy}
							</div>
							<div class="message-timestamp">
								{new Date(msg.createdAt).toLocaleString([], {
									dateStyle: 'short',
									timeStyle: 'short'
								})}
							</div>
						</div>
						<div>
							{msg.content}
						</div>
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
		display: flex;
		flex-direction: column;
		gap: 5px;
	}

	.message-header {
		display: flex;
		gap: 20px;
	}

	.message-username {
		font-size: medium;
	}

	.message-timestamp {
		font-size: small;
	}
</style>
