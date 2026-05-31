<script lang="ts">
	import { getMotd } from '$lib/backend.remote';
	import { commandService } from '$lib/command-service.svelte';
	import { websocketService } from '$lib/websocket.svelte';

	let inputField: HTMLElement;
	const messages = $derived(commandService.messages);

	let inputText = $state('');
	let viewport = $state<HTMLDivElement>();
	let autoscroll = $state(true);

	const sendMessage = (e: SubmitEvent) => {
		e.preventDefault();
		commandService.newMessage(inputText);
		inputField.focus();
		inputText = '';
	};

	$effect(() => {
		if (!viewport) return;
		if (messages.length <= 0) return;
		if (!messages[messages.length - 1]) return;
		if (!autoscroll) return;
		viewport.scrollTo(0, viewport.scrollHeight);
	});
</script>

<div class="wrapper">
	<h3>
		Message of the Day:

		<svelte:boundary>
			{await getMotd()}
			{#snippet pending()}
				Loading...
			{/snippet}
		</svelte:boundary>
	</h3>

	<h3>Server status: {websocketService.status}</h3>

	<div class="messages-container">
		<div
			class="chatbox-outer"
			bind:this={viewport}
			onscroll={() => {
				if (!viewport) return;
				autoscroll = viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop < 1;
			}}
		>
			<div class="chatbox-inner">
				{#each messages.toReversed() as msg (msg.id)}
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
		<form onsubmit={sendMessage}>
			<input
				class="message-input"
				type="text"
				placeholder="Type a message..."
				bind:value={inputText}
				bind:this={inputField}
			/>
			<button type="submit">Send message</button>
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
