<script lang="ts">
	let { children } = $props();
</script>

{@render children()}

<!-- // <div class="wrapper"> -->
<!-- // 	<div class="channel-wrapper"></div> -->
<!-- // </div> -->

<style>
	.channel-entry-container {
		display: flex;
	}

	.channel-entry:hover {
		color: var(--text-normal);
	}
	.channel-entry {
		border-radius: 5px;
		width: 100%;
		font-size: medium;
		text-align: left;
		padding: 4px 10px;
		color: var(--text-muted);
		text-decoration: none;
	}

	.text-overflow {
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.selected {
		background-color: var(--background-secondary);
		color: var(--text-normal);
	}
	.channel-list {
		display: flex;
		flex-direction: column;
	}
	.side-panel {
		flex-shrink: 0;
		display: flex;
		box-sizing: border-box;
		flex-direction: column;
		min-width: 100px;
		max-width: 300px;
		padding-inline: 5px;
	}
	.side-panel-slider {
		flex-shrink: 0;
		width: 5px;
		background-color: var(--background-secondary);
		cursor: col-resize;
	}
	.dragging {
		background-color: var(--background-accent);
	}
	.channel-wrapper {
		display: flex;
		flex-grow: 1;
		max-width: 100%;
		min-width: 0;
	}
	.wrapper {
		display: flex;
		height: 100%;
	}
</style>
