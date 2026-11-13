#!/usr/bin/env python3
"""
Tests manuels scénarisés TM-01 à TM-08 (chapitre 7), rejoués automatiquement avec Playwright.
Produit des captures datées et un compte rendu Markdown dans docs/tests/<date>/tm/.
Prérequis : API sur :8080 (données de démonstration, EMAIL_ENABLED=true vers un serveur SMTP local :1025
qui écrit /tmp/mails/mails.jsonl), interface sur :4200.
Usage : python3 tm_tests.py
"""
import asyncio, json, os, re, time, urllib.request
from pathlib import Path
from playwright.async_api import async_playwright, expect

BASE = 'http://localhost:4200'; API = 'http://localhost:8080'
DATE = time.strftime('%Y-%m-%d'); STAMP = time.strftime('%Y-%m-%d %H:%M')
OUT = Path(__file__).resolve().parents[2] / 'docs' / 'tests' / DATE / 'tm'; OUT.mkdir(parents=True, exist_ok=True)
PWD = 'Demo@2026!'
ACCOUNTS = {'admin': 'admin@senetech.example', 'manager': 'manager@senetech.example', 'tech': 'tech.materiel@senetech.example', 'tech2': 'tech.reseau@senetech.example', 'user': 'user@client.example'}
results = []

def api(method, path, token=None, body=None):
    req = urllib.request.Request(API + path, data=json.dumps(body).encode() if body is not None else None, method=method)
    req.add_header('Content-Type', 'application/json')
    if token: req.add_header('Authorization', 'Bearer ' + token)
    with urllib.request.urlopen(req) as r: raw = r.read().decode(); return json.loads(raw) if raw else None

def api_token(role): return api('POST', '/api/auth/login', body={'email': ACCOUNTS[role], 'password': PWD})['accessToken']

def record(tid, scenario, expected, observed, ok, shots):
    results.append({'id': tid, 'scenario': scenario, 'expected': expected, 'observed': observed, 'status': 'Réussi' if ok else 'ÉCHEC', 'shots': shots})
    print(f"[{tid}] {'OK ' if ok else 'KO '} {observed}")

async def login(page, role):
    await page.goto(BASE + '/login'); await page.wait_for_selector('input[type=email]')
    await page.fill('input[type=email]', ACCOUNTS[role]); await page.fill('input[formcontrolname=password]', PWD)
    await page.click('button[type=submit]'); await page.wait_for_url(lambda u: '/login' not in u, timeout=20000); await page.wait_for_timeout(1200)

async def logout(page):
    await page.evaluate("localStorage.clear()"); await page.goto(BASE + '/login'); await page.wait_for_timeout(400)

async def shot(page, name, full=False):
    f = f"{name}.png"; await page.screenshot(path=str(OUT / f), full_page=full); return f

async def select_option(page, field_label, option_text, scope=None):
    """Ouvre un mat-select identifié par son libellé et choisit l'option contenant le texte."""
    fld = (scope or page).locator('mat-form-field', has_text=field_label).first
    await fld.locator('mat-select').click(); await page.wait_for_timeout(300)
    await page.locator('mat-option', has_text=option_text).first.click(); await page.wait_for_timeout(300)

async def dismiss_snack(page):
    btn = page.locator('.mat-mdc-snack-bar-action')
    if await btn.count() > 0:
        try: await btn.first.click(timeout=1500); await page.wait_for_timeout(400)
        except Exception: pass

async def snack_text(page):
    try:
        el = page.locator('.mdc-snackbar__label').first; await el.wait_for(timeout=6000); txt = (await el.inner_text()).strip()
        await dismiss_snack(page); return txt
    except Exception: return ''

async def fill_label(page, label, value):
    fld = page.locator('mat-form-field', has_text=label).first
    await fld.locator('input, textarea').first.fill(value)

# ---------------------------------------------------------------- scénarios
async def tm01(page):
    shots = []; obs = []
    expected_nav = {'user': ['Tickets', 'Base de connaissances'], 'tech': ['Tableau de bord', 'Tickets', 'Planning', 'Équipements', 'Base de connaissances'],
                    'manager': ['Tableau de bord', 'Tickets', 'Planning', 'Équipements', 'Base de connaissances', 'Propositions IA'],
                    'admin': ['Tableau de bord', 'Tickets', 'Planning', 'Équipements', 'Base de connaissances', 'Propositions IA', 'Administration']}
    ok = True
    for role, exp in expected_nav.items():
        await login(page, role); await page.wait_for_timeout(800)
        items = [t.strip() for t in await page.locator('.nav-item .lbl').all_inner_texts()]
        good = items == exp; ok &= good; obs.append(f"{role} : {', '.join(items)}{'' if good else ' (ATTENDU ' + ', '.join(exp) + ')'}")
        shots.append(await shot(page, f'TM-01-{role}'))
        await page.click('.user-btn'); await page.locator('button', has_text='Déconnexion').click(); await page.wait_for_url('**/login', timeout=10000)
    obs.append('déconnexion : retour à /login pour chaque rôle')
    record('TM-01', 'Connexion avec chaque compte de démonstration ; menu adapté au rôle ; déconnexion', 'Navigation conforme à la matrice des droits', ' ; '.join(obs), ok, shots)

async def tm02(page):
    await login(page, 'admin'); await page.goto(BASE + '/admin'); await page.wait_for_timeout(1000)
    await page.locator('.mat-mdc-tab', has_text='Politiques SLA').click(); await page.wait_for_timeout(800)
    card = page.locator('mat-card', has_text='P2').first
    await card.locator('mat-form-field', has_text='Prise en compte').locator('input').fill('120')
    await card.locator('mat-form-field', has_text='Résolution').locator('input').fill('60')
    await card.locator('button', has_text='Enregistrer').click(); err = await snack_text(page); s1 = await shot(page, 'TM-02-refus')
    await page.wait_for_timeout(500)
    await card.locator('mat-form-field', has_text='Résolution').locator('input').fill('480')
    await card.locator('button', has_text='Enregistrer').click(); okmsg = await snack_text(page); s2 = await shot(page, 'TM-02-valide')
    # restauration de la valeur par défaut (60 / 480)
    await page.wait_for_timeout(3500); await card.locator('mat-form-field', has_text='Prise en compte').locator('input').fill('60'); await card.locator('button', has_text='Enregistrer').click(); await page.wait_for_timeout(1000)
    ok = 'inférieur' in err.lower() or 'supérieur' in err.lower(); ok2 = 'mise à jour' in okmsg.lower()
    record('TM-02', "L'administrateur modifie la politique P2 (résolution < prise en compte) puis une valeur valide", 'Message d\'erreur explicite ; puis enregistrement',
           f"refus : « {err} » ; valide : « {okmsg} »", ok and ok2, [s1, s2]); await logout(page)

async def tm03(page):
    tok = api_token('manager')
    tickets = api('GET', '/api/tickets?openOnly=true&size=100', tok)['content']
    ref = next(t for t in tickets if t['status'] in ('NEW', 'ASSIGNED', 'IN_PROGRESS'))['reference']
    await login(page, 'manager'); await page.goto(BASE + '/planning'); await page.wait_for_timeout(1000)
    # créneau : lundi prochain 09:00-11:00
    import datetime as dt
    d = dt.date.today() + dt.timedelta(days=(7 - dt.date.today().weekday()) % 7 or 7)
    s_a, e_a = f"{d}T09:00", f"{d}T11:00"; s_b, e_b = f"{d}T10:00", f"{d}T12:00"
    async def plan(start, end):
        if await page.locator('mat-card', has_text='Ticket ouvert').count() == 0:
            await page.locator('.page-header button', has_text='Planifier').first.click(); await page.wait_for_timeout(500)
        form = page.locator('mat-card', has_text='Ticket ouvert').first
        await select_option(page, 'Ticket ouvert', ref, scope=form); await select_option(page, 'Technicien', 'Fatou Bâ', scope=form); await select_option(page, 'Mode', 'Sur site', scope=form)
        await form.locator('mat-form-field', has_text='Début').locator('input').fill(start); await form.locator('mat-form-field', has_text='Fin').locator('input').fill(end)
        await form.locator('button', has_text='Enregistrer').click(); return await snack_text(page)
    m1 = await plan(s_a, e_a); await page.wait_for_timeout(3500); s1 = await shot(page, 'TM-03-planifiee')
    m2 = await plan(s_b, e_b); s2 = await shot(page, 'TM-03-conflit'); await page.wait_for_timeout(3500)
    # compte rendu
    await page.goto(BASE + '/planning'); await page.wait_for_timeout(800)
    # naviguer jusqu'à la semaine du créneau
    for _ in range(3):
        if await page.locator('table tr', has_text=ref).count() > 0: break
        await page.locator('button:has(mat-icon:text("chevron_right"))').click(); await page.wait_for_timeout(700)
    row = page.locator('table tr', has_text=ref).first; await row.locator('button', has_text='Compte rendu').click(); await page.wait_for_timeout(500)
    await select_option(page, 'État', 'Terminée'); await page.locator('mat-card button', has_text='Enregistrer').last.click(); m3 = await snack_text(page); s3 = await shot(page, 'TM-03-cr-vide')
    await page.wait_for_timeout(3500)
    await page.locator('mat-form-field', has_text='Compte rendu').locator('textarea').fill('Imprimante remise en service ; pilote réinstallé, test OK avec l\'utilisateur.')
    await page.locator('mat-card button', has_text='Enregistrer').last.click(); m4 = await snack_text(page); s4 = await shot(page, 'TM-03-cr-ok')
    ok = 'planifiée' in m1.lower() and 'conflit' in m2.lower() and 'obligatoire' in m3.lower() and 'enregistré' in m4.lower()
    record('TM-03', 'Planifier deux interventions chevauchantes pour le même technicien ; saisir un compte rendu vide puis complet', 'Refus du chevauchement ; refus du compte rendu vide ; intervention terminée',
           f"1re planification : « {m1} » ; 2e (10h-12h) : « {m2} » ; CR vide : « {m3} » ; CR complet : « {m4} »", ok, [s1, s2, s3, s4]); await logout(page)

async def tm04(page):
    tok = api_token('manager'); open_api = api('GET', '/api/tickets?openOnly=true&size=1', tok)['totalElements']
    await login(page, 'manager'); await page.goto(BASE + '/dashboard'); await page.wait_for_timeout(2500)
    shown = (await page.locator('.kpi-card', has_text='Tickets ouverts').locator('.kpi-value').inner_text()).strip()
    await select_option(page, 'Période', '7 jours'); await page.wait_for_timeout(2000); s1 = await shot(page, 'TM-04-7j', True)
    created7 = api('GET', '/api/dashboard/kpis?days=7', tok)['createdLast30Days']
    txt = (await page.locator('.kpi-card', has_text='Créés / résolus').locator('.kpi-value').inner_text()).replace(' ', '')
    ok = shown == str(open_api) and txt.startswith(str(created7))
    record('TM-04', "Changer la période du tableau de bord ; vérifier un indicateur à la main", 'Valeurs cohérentes avec le calcul manuel',
           f"tickets ouverts affichés {shown} = liste filtrée openOnly {open_api} ; créés sur 7 j affichés « {txt} » vs API {created7}", ok, [s1]); await logout(page)

async def tm05(page):
    await login(page, 'tech'); await page.goto(BASE + '/equipment'); await page.wait_for_timeout(1000)
    await page.locator('button', has_text='Ajouter').click(); await page.wait_for_timeout(400)
    await fill_label(page, "N° inventaire", 'BAS-PC-001'); await select_option(page, 'Site', 'Agence Plateau'); await fill_label(page, 'Marque', 'Test')
    await page.locator('mat-card button', has_text='Enregistrer').click(); m1 = await snack_text(page); s1 = await shot(page, 'TM-05-doublon')
    await page.locator('button', has_text='Annuler').click(); await page.wait_for_timeout(3500)
    await select_option(page, 'Site', 'Usine Rufisque', scope=page.locator('.page-header')); await page.wait_for_timeout(800)
    sites = set((await page.locator('table tbody tr td:nth-child(4)').all_inner_texts()))
    expired = await page.locator('table tbody tr td.sla-breach').count(); s2 = await shot(page, 'TM-05-filtre')
    ok = 'existant' in m1.lower() and sites == {'Usine Rufisque'} and expired >= 1
    record('TM-05', 'Créer un équipement avec un numéro existant ; filtrer par site ; garantie expirée', 'Refus du doublon ; filtre correct ; alerte visuelle',
           f"doublon : « {m1} » ; sites affichés après filtre : {sorted(sites)} ; lignes avec garantie expirée signalées : {expired}", ok, [s1, s2]); await logout(page)

async def tm06(page):
    await login(page, 'tech'); await page.goto(BASE + '/kb'); await page.wait_for_timeout(800)
    await page.locator('.page-header button', has_text='Rédiger').click(); await page.wait_for_timeout(400)
    editor = page.locator('mat-card', has_text='Contenu (procédure').first
    await editor.locator('mat-form-field', has_text='Titre').locator('input').fill('Brouillon TM-06 (non publié)'); await editor.locator('mat-form-field', has_text='Contenu').locator('textarea').fill('Contenu de test, article non publié.')
    await editor.locator('button', has_text='Enregistrer').click(); saved = await snack_text(page); await page.wait_for_timeout(800); print('   TM-06 enregistrement :', saved)
    tech_sees = await page.locator('mat-card', has_text='Brouillon TM-06').count(); s1 = await shot(page, 'TM-06-technicien')
    await logout(page); await login(page, 'user'); await page.goto(BASE + '/kb'); await page.wait_for_timeout(800)
    await page.locator('mat-form-field', has_text='Rechercher').locator('input').fill('imprimante'); await page.keyboard.press('Enter'); await page.wait_for_timeout(1000)
    titles = await page.locator('mat-card mat-card-title').all_inner_texts(); s2 = await shot(page, 'TM-06-demandeur')
    user_sees_draft = await page.locator('mat-card', has_text='Brouillon TM-06').count()
    await page.locator('mat-form-field', has_text='Rechercher').locator('input').fill(''); await page.keyboard.press('Enter'); await page.wait_for_timeout(800)
    user_sees_draft_all = await page.locator('mat-card', has_text='Brouillon TM-06').count()
    ok = tech_sees >= 1 and any('imprimante' in t.lower() for t in titles) and all('imprimante' in t.lower() or 'poste' in t.lower() for t in titles) and user_sees_draft == 0 and user_sees_draft_all == 0
    record('TM-06', 'Rechercher « imprimante » en demandeur et en technicien ; article non publié', "Résultats pertinents ; l'article non publié n'apparaît qu'au personnel",
           f"technicien voit le brouillon : {tech_sees >= 1} ; résultats demandeur « imprimante » : {titles} ; brouillon visible du demandeur : {bool(user_sees_draft_all)}", ok, [s1, s2]); await logout(page)

async def tm07(page):
    Path('/tmp/mails/mails.jsonl').unlink(missing_ok=True)
    await login(page, 'user'); await page.goto(BASE + '/tickets/new'); await page.wait_for_timeout(800)
    await fill_label(page, 'Titre', 'Test TM-07 notifications e-mail'); await page.locator('mat-form-field', has_text='Description').locator('textarea').fill('Ticket créé pour vérifier les notifications de création, affectation et résolution.')
    await page.locator('button', has_text='Créer le ticket').click(); await page.wait_for_url('**/tickets/*', timeout=10000); await page.wait_for_timeout(1000)
    ref = (await page.locator('.ref-chip').inner_text()).strip(); tid = page.url.rstrip('/').split('/')[-1]
    s1 = await shot(page, 'TM-07-creation'); await logout(page)
    mtok = api_token('manager'); techs = api('GET', '/api/users/technicians', mtok); fatou = next(t for t in techs if 'Fatou' in t['fullName'])
    await login(page, 'manager'); await page.goto(f"{BASE}/tickets/{tid}"); await page.wait_for_timeout(1200)
    await select_option(page, 'Affecter à', 'Fatou'); await page.locator('button', has_text='Affecter').click(); await snack_text(page); await page.wait_for_timeout(800); await logout(page)
    await login(page, 'tech'); await page.goto(f"{BASE}/tickets/{tid}"); await page.wait_for_timeout(1200)
    await page.locator('button', has_text='Prendre en charge').click(); await page.wait_for_timeout(400); await page.locator('button', has_text='Confirmer').click(); await snack_text(page); await page.wait_for_timeout(3500)
    await page.locator('button', has_text='Résoudre').click(); await page.wait_for_timeout(400)
    await page.locator('mat-form-field', has_text='Résumé de la résolution').locator('textarea').fill('Problème reproduit puis corrigé ; validation avec le demandeur.')
    await page.locator('button', has_text='Confirmer').click(); await snack_text(page); await page.wait_for_timeout(1500); s2 = await shot(page, 'TM-07-resolu', True)
    await page.wait_for_timeout(1500)
    mails = [json.loads(l) for l in open('/tmp/mails/mails.jsonl')] if Path('/tmp/mails/mails.jsonl').exists() else []
    subj = [m['subject'] for m in mails if ref in m['subject']]
    ok = any('enregistrée' in s for s in subj) and any('affecté' in s for s in subj) and any('résolu' in s for s in subj)
    record('TM-07', 'Créer, affecter, résoudre un ticket avec EMAIL_ENABLED=true ; consulter les e-mails capturés', 'Trois e-mails reçus avec la référence',
           f"{len(subj)} e-mail(s) portant {ref} : {subj}", ok, [s1, s2]); await logout(page)

async def tm08(browser):
    shots = []; obs = []
    ctx = await browser.new_context(viewport={'width': 1280, 'height': 800}, locale='fr-FR'); page = await ctx.new_page(); await login(page, 'manager')
    await page.goto(BASE + '/tickets'); await page.wait_for_timeout(1500); shots.append(await shot(page, 'TM-08-1280'))
    badges = await page.locator('table .badge').all_inner_texts(); ok1 = all(b.strip() for b in badges); obs.append(f"1280 px : {len(badges)} badges, tous avec texte : {ok1}")
    await ctx.close()
    ctx = await browser.new_context(viewport={'width': 420, 'height': 860}, locale='fr-FR', device_scale_factor=2); page = await ctx.new_page(); await login(page, 'user')
    await page.goto(BASE + '/tickets'); await page.wait_for_timeout(1500); shots.append(await shot(page, 'TM-08-420'))
    width = await page.evaluate('document.documentElement.scrollWidth'); obs.append(f"420 px (demandeur) : largeur de page {width} px")
    await page.goto(BASE + '/tickets/new'); await page.wait_for_timeout(1200); shots.append(await shot(page, 'TM-08-420-form', True))
    await ctx.close()
    record('TM-08', "Parcours sur un écran de 1 280 px et sur une fenêtre étroite ; lecture des badges", 'Interface utilisable ; états lisibles par le texte', ' ; '.join(obs) + ' ; sur mobile la navigation latérale reste affichée (limite connue : pas de menu repliable automatique)', ok1, shots)

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(args=['--no-sandbox'])
        ctx = await browser.new_context(viewport={'width': 1440, 'height': 900}, locale='fr-FR', device_scale_factor=1.25); page = await ctx.new_page()
        import sys
        only = sys.argv[1:]
        for fn in (tm01, tm02, tm03, tm04, tm05, tm06, tm07):
            if only and fn.__name__ not in only: continue
            try: await fn(page)
            except Exception as e:
                f = await shot(page, f'{fn.__name__.upper()}-erreur'); record(fn.__name__.upper().replace('TM0', 'TM-0'), fn.__doc__ or fn.__name__, '', f'exception : {type(e).__name__}: {str(e)[:200]}', False, [f])
        await ctx.close()
        if not only or 'tm08' in only:
            try: await tm08(browser)
            except Exception as e: record('TM-08', '', '', f'exception : {e}', False, [])
        await browser.close()
    with open(OUT / 'TM-resultats.md', 'w', encoding='utf-8') as f:
        f.write(f"# Tests manuels scénarisés rejoués automatiquement — {STAMP}\n\nEnvironnement : API locale (JAR) + PostgreSQL 16, interface Angular (ng serve), Playwright/Chromium, données de démonstration fictives, SMTP de capture local.\n\n| Id | Résultat observé | Statut | Captures |\n|---|---|---|---|\n")
        for r in results: f.write(f"| {r['id']} | {r['observed']} | {r['status']} | {', '.join(r['shots'])} |\n")
    json.dump(results, open(OUT / 'TM-resultats.json', 'w'), ensure_ascii=False, indent=1)
    print(f"\n{sum(r['status']=='Réussi' for r in results)}/{len(results)} réussis — {OUT}")

asyncio.run(main())
