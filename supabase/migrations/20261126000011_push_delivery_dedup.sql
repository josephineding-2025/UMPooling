-- Service-only delivery leases support safe webhook retries. No client tokens
-- or message content are stored here. FCM cannot guarantee exactly-once delivery.
begin;
create table public.push_deliveries (
    event_key text not null,
    user_id uuid not null references public.users("usersId"),
    status text not null check(status in ('sending','sent','failed','discarded')),
    lease_until timestamptz not null,
    attempts integer not null default 1,
    updated_at timestamptz not null default now(),
    primary key(event_key,user_id)
);
alter table public.push_deliveries enable row level security;
revoke all on public.push_deliveries from public,anon,authenticated;
grant select,insert,update on public.push_deliveries to service_role;
create or replace function public.claim_push_delivery(p_event_key text,p_user_id uuid)
returns boolean language plpgsql security definer set search_path = ''
as $function$
declare v_claimed integer;
begin
    insert into public.push_deliveries(event_key,user_id,status,lease_until)
    values(p_event_key,p_user_id,'sending',now()+interval '2 minutes')
    on conflict(event_key,user_id) do update set status='sending',
        lease_until=now()+interval '2 minutes',attempts=public.push_deliveries.attempts+1,updated_at=now()
    where public.push_deliveries.status='failed'
       or (public.push_deliveries.status='sending' and public.push_deliveries.lease_until<now());
    get diagnostics v_claimed=row_count;
    return v_claimed=1;
end;
$function$;
revoke all on function public.claim_push_delivery(text,uuid) from public,anon,authenticated;
grant execute on function public.claim_push_delivery(text,uuid) to service_role;
-- Register a token atomically to one current account, including on a shared
-- device after an earlier offline logout could not clear its old profile.
create or replace function public.register_fcm_token(p_token text)
returns void language plpgsql security definer set search_path=''
as $function$
declare v_user uuid:=auth.uid();
begin
    if v_user is null then raise exception 'Sign in first';end if;
    if p_token is null or btrim(p_token)='' or length(p_token)>4096 then raise exception 'Invalid device token';end if;
    perform pg_catalog.pg_advisory_xact_lock(873214,4);
    if not exists(select 1 from public.users where "usersId"=v_user) then raise exception 'User profile is missing';end if;
    update public.users set fcm_token=null where fcm_token=p_token and "usersId"<>v_user;
    update public.users set fcm_token=p_token where "usersId"=v_user;
end;
$function$;
create or replace function public.unregister_fcm_token()
returns void language plpgsql security definer set search_path=''
as $function$
begin
    if auth.uid() is null then raise exception 'Sign in first';end if;
    perform pg_catalog.pg_advisory_xact_lock(873214,4);
    update public.users set fcm_token=null where "usersId"=auth.uid();
end;
$function$;
revoke update(fcm_token) on public.users from public,anon,authenticated;
revoke all on function public.register_fcm_token(text) from public,anon;
revoke all on function public.unregister_fcm_token() from public,anon;
grant execute on function public.register_fcm_token(text) to authenticated;
grant execute on function public.unregister_fcm_token() to authenticated;
commit;
