<script>
	import { resolve } from '$app/paths';
	import { DEFAULT_GUILD } from '$lib/const';

	let { class: className = '' } = $props();
</script>

<aside class="root {className}">
	<div class="scroll-wrapper">
		<ol>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>a</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>a</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>a</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>a</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>a</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>b</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>c</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>d</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>e</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>b</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>c</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>d</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>e</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>b</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>c</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>d</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>e</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>b</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>c</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>d</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>e</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>b</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>c</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>d</a></li>
			<li><a href={resolve(`/channels/${DEFAULT_GUILD}/`)}>e</a></li>
		</ol>
	</div>
</aside>

<style>
	.root {
		/* border: 1px solid var(--accent); */
		/* border-radius: 16px; */
		height: 100%;
		width: 100%;
		overflow: scroll;
	}
	.scroll-wrapper {
		display: flex;
		justify-content: center;
	}
	ol {
		display: flex;
		flex-direction: column;
		list-style-type: none;
		margin-left: 0px;
		padding-left: 0px;
		gap: 8px;
	}
	li {
		display: flex;
		width: 48px;
		height: 48px;
		border: 1px solid var(--accent);
		border-radius: 16px;
		justify-content: center;
		align-items: center;
	}
	a {
		text-decoration: none;
		color: var(--foreground);
	}
</style>
