<script lang="ts">
	import { register } from '$lib/backend.remote';
</script>

<form {...register}>
	<h3>Sign up</h3>
	<div>
		<label>Email <input {...register.fields.email.as('email')} /> </label>
	</div>
	<div>
		<label>Username <input {...register.fields.username.as('text')} /> </label>
	</div>
	<div>
		<label>Password <input {...register.fields.password.as('password')} /> </label>
	</div>
	{#each register.fields.allIssues() as issue}
		<div>
			{issue.path}
			{issue.message}
		</div>
	{/each}
	<button>Sign up</button>
</form>
