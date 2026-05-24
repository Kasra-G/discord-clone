export interface Message {
	readonly id: string;
	readonly createdAt: string;
	readonly updatedAt: string;
	readonly channelId: string;
	readonly content: string;
	readonly sentBy: string;
}

export interface ClientCommand {
	readonly command: String;
	readonly timestamp: String;
	readonly payload: object;
}
