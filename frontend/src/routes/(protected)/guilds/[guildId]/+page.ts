import { getChannels } from '$lib/backend.remote';
import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import { resolve } from '$app/paths';

export const load: PageLoad = async ({ params }) => {
	const channels = await getChannels({ guildId: params.guildId });
	redirect(308, resolve(`/guilds/${params.guildId}/channels/${channels[0].id}`));
};
