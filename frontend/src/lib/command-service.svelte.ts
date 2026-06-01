import { getMessages } from './backend.remote';
import type { Message } from './model';
import { websocketService } from './websocket.svelte';

class CommandService {
	constructor() {
		websocketService.onMessage((msg) => {
			if (msg.command === 'NEW_MESSAGE') this.messages.push(msg.payload);
		});

		websocketService.onOpen(() => {
			websocketService.send({
				userId: '019e822b-5e70-75a4-8e7d-dc8cfc7e3983'
			});
		});
	}

	async loadMessages() {
		const savedMessages = await getMessages({ channelId: 'default', count: 20 });
		this._messages = savedMessages.toReversed();
	}

	private _messages: Message[] = $state([]);
	public get messages() {
		return this._messages;
	}

	newMessage(message: string) {
		const command = {
			command: 'BROADCAST_MESSAGE',
			message: message
		};
		websocketService.send(command);
	}
}

export const commandService = new CommandService();
