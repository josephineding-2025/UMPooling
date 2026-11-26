-- OPTIONAL DEPLOYMENT STEP: enable pg_cron through the Supabase extensions UI
-- first. Requires migration 11, deployed swift-worker and matching Vault/Edge
-- WEBHOOK_SECRET. No credential belongs in this file or a cron command.
begin;
create or replace function public.retry_pending_push()
returns void language plpgsql security definer set search_path=''
as $function$
declare v_secret text;
begin
    select decrypted_secret into v_secret from vault.decrypted_secrets where name='webhook_secret';
    if v_secret is null or v_secret='' then raise exception 'Configure the webhook_secret in Vault';end if;
    perform net.http_post(
        url:='https://trzzdcsfpsdlxxwvuufx.supabase.co/functions/v1/swift-worker',
        headers:=jsonb_build_object('Content-Type','application/json','x-webhook-secret',v_secret),
        body:='{"mode":"retry"}'::jsonb,timeout_milliseconds:=5000
    );
end;
$function$;
revoke all on function public.retry_pending_push() from public,anon,authenticated;
do $$begin
    if not exists(select 1 from pg_catalog.pg_extension where extname='pg_cron') then
        raise exception 'Enable pg_cron before scheduling retries';end if;
end$$;
-- Re-running schedule with the same name updates this job instead of adding duplicates.
select cron.schedule('umpooling-push-retry','* * * * *','select public.retry_pending_push();');
commit;
