<script lang="ts">
	import { beforeNavigate } from '$app/navigation';
	import { getChannel, getMessages, sendMessage } from '$lib/backend.remote';
	import { messageService } from '$lib/message-service.svelte';
	import { deep } from '$lib/util.svelte.js';
	import { onMount } from 'svelte';
	import * as schemas from '$lib/schemas';
	import { showErrors } from '$lib/snippets.svelte';

	let { params } = $props();

	let inputText = $state('');
	let messages = $derived(
		deep(await getMessages({ guildId: params.guildId, channelId: params.channelId, count: 100 }))
	);

	beforeNavigate(({ to }) => {
		const channelId = to?.params?.channelId;
		const guildId = to?.params?.guildId;
		if (channelId && guildId) {
			void getMessages({
				guildId: guildId,
				channelId: channelId,
				count: 100
			}).refresh();
		}
	});

	// todo message sending is very broken

	onMount(() => {
		return messageService.subscribeOnMessage((msg) => {
			if (msg.channelId === params.channelId) {
				messages.push(msg);
			}
		});
	});

	let scroller = $state<DOMRect>();
	let autoscroll = $state(true);
	const autoscrollToLast = (elem: HTMLElement) => {
		if (!scroller) return;
		if (scroller.width <= 0) return;
		if (scroller.height <= 0) return;
		if (messages.length <= 0) return;
		if (!autoscroll) return;
		elem.lastElementChild?.scrollIntoView();
	};

	const getChannelDetails = () => {
		return getChannel({ guildId: params.guildId, channelId: params.channelId });
	};
</script>

<div class="messages-container">
	<div class="channel-header">
		<h4>#{(await getChannelDetails()).name}</h4>
		<h5>{(await getChannelDetails()).description}</h5>
	</div>
	<div
		bind:contentRect={scroller}
		class="chatbox-outer"
		onwheel={({ currentTarget }) => {
			autoscroll =
				currentTarget.scrollHeight - currentTarget.clientHeight - currentTarget.scrollTop < 50;
		}}
	>
		<div class="chatbox-inner" {@attach autoscrollToLast}>
			{#each messages as msg (msg.id)}
				<div class="message">
					<div class="header">
						<div class="username">
							{msg.author.username}
						</div>
						<div class="timestamp">
							{new Date(msg.createdAt).toLocaleString([], {
								dateStyle: 'short',
								timeStyle: 'short'
							})}
						</div>
					</div>
					<div class="content">
						{msg.content}
					</div>
				</div>
			{/each}
		</div>
	</div>
	<form
		{...sendMessage.preflight(schemas.SEND_MESSAGE).enhance(async (form) => {
			if (await form.submit().updates()) {
				form.element.reset();
				inputText = '';
			}
		})}
		oninput={() => sendMessage.validate()}
		class="message-input-container"
	>
		<div class="input-error-wrapper">
			{@render showErrors(sendMessage.fields.message)}
		</div>
		<div
			tabindex="0"
			role="textbox"
			contenteditable="true"
			class="message-input"
			placeholder={`Message #${(await getChannelDetails()).name}`}
			bind:innerText={() => inputText, (e) => (inputText = e === '\n' ? '' : e)}
			onkeydown={(e) => {
				if (e.key === 'Enter' && !e.shiftKey) {
					e.preventDefault();
					sendMessage.element?.requestSubmit();
					e.currentTarget.focus();
				}
			}}
		></div>
		<input {...sendMessage.fields.message.as('hidden', inputText.trim())} />
		<input {...sendMessage.fields.channelId.as('hidden', params.channelId)} />
		<input {...sendMessage.fields.guildId.as('hidden', params.guildId)} />
	</form>
</div>

<style>
	.messages-container {
		display: flex;
		flex-direction: column;
		height: 100%;
	}

	.input-error-wrapper {
		margin-bottom: 4px;
		padding-left: 4px;
		font-size: 0.85rem;
	}

	.channel-header {
		padding-left: 10px;
		border-bottom: 1px solid var(--accent);
	}

	.chatbox-outer {
		overflow-y: auto;
		flex-grow: 1;
		border-top: 1px solid var(--background-accent);
	}

	.chatbox-inner {
		display: flex;
		flex-direction: column;
		justify-content: flex-end;
		min-height: 100%;
	}

	.message {
		margin: 0px 4px 4px;
		border: 1px solid var(--accent);
		border-radius: 12px;
		padding: 12px;
		background-color: var(--background);
		color: var(--foreground);
		display: flex;
		flex-direction: column;
		gap: 4px;

		.header {
			display: flex;
			align-items: center;
			gap: 20px;

			.username {
				font-size: large;
				color: var(--primary);
			}
			.timestamp {
				font-size: small;
				color: var(--muted-foreground);
			}
		}
		.content {
			font-size: medium;
			text-wrap-mode: wrap;
			overflow-wrap: break-word;
			white-space: break-spaces;
			text-wrap-style: pretty;
			vertical-align: baseline;
		}
	}

	.message-input-container {
		display: flex;
		flex-direction: column;

		.message-input {
			border: 1px solid var(--accent);
			color: var(--foreground);
			min-height: 56px;
			padding: 18px 12px 16px 20px;
			max-height: 200px;
			field-sizing: content;

			border-radius: 12px;
			background-color: var(--input);
			margin: 8px;

			font-size: medium;
			overflow-y: scroll;
			outline: none;
			word-break: break-all;
		}
		div[contenteditable='true'] {
			&[placeholder]:empty::before {
				content: attr(placeholder);
				color: var(--muted-foreground);
				cursor: text;
				user-select: none;
			}
		}
	}
</style>
