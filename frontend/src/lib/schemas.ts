import { PUBLIC_BACKEND_URL } from '$env/static/public';
import z from 'zod';

export const GET_MESSAGES = z.object({
	channelId: z.string(),
	count: z.number().positive().lte(100)
});

export const REGISTER = z
	.object({
		username: z
			.string()
			.nonempty('Please specify a username')
			.min(3, 'Username must be at least length 3'),
		email: z.email().nonempty('Please specify an email'),
		_password: z
			.string()
			.min(8, { message: 'Password must be at least 8 characters long' })
			.max(32, { message: 'Password cannot exceed 32 characters' })
			.regex(/[A-Z]/, { message: 'Password must contain at least one uppercase letter' })
			.regex(/[a-z]/, { message: 'Password must contain at least one lowercase letter' })
			.regex(/[0-9]/, { message: 'Password must contain at least one number' })
			.regex(/[^A-Za-z0-9]/, { message: 'Password must contain at least one special character' }),
		_confirmPassword: z.string().nonempty('Please confirm the password')
	})
	.refine((data) => data._password === data._confirmPassword, {
		error: 'Password does not match',
		path: ['_confirmPassword']
	});

export const LOGIN = z.object({
	username: z.string().nonempty('Please specify a username'),
	_password: z.string().nonempty('Please specify a password')
});

export const SEND_MESSAGE = z.object({
	message: z.string().nonempty(' ').max(255, 'Message too long'),
	channelId: z.string().nonempty()
});

export const GET_CHANNELS = z.object({
	guildId: z.string().nonempty()
});

export const GET_CHANNEL = z.object({
	channelId: z.string().nonempty()
});

export interface UserDetails {
	readonly id: string;
	readonly username: string;
	readonly createdAt: string;
	readonly updatedAt: string;
	readonly email: string;
}

export const BASE_API_URL = `${PUBLIC_BACKEND_URL}/api`;
