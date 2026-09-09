import type { DomainLanguage } from '~~/shared/utils/domain-language'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

interface StaticFullPageEntry {
  pageTitle: string
  metaTitle: string
  metaDescription: string
  width: 'container' | 'container-fluid' | 'container-semi-fluid'
  html: Partial<Record<DomainLanguage, string>>
}

/**
 * Real editorial copy for the XWiki full pages that used to be fetched live, extracted
 * 2026-09-09 from the owner-provided export archive (`/opt/open4goods/backup/xwiki-export.xar`)
 * rather than the live wiki -- anonymous XWiki REST access is down (see
 * incident_xwiki_blog_locked_down_sept2026), and authenticated access with the app's own
 * `xwiki.username`/`xwiki.password` service account (application-devsec.yml) redirects to a
 * login page too, confirming the lockdown is not merely an anonymous-access restriction.
 *
 * French only: the source has no English translation for either page, and `getStaticFullPage`
 * falls back to French rather than inventing one, consistent with static-content-blocs.ts.
 */
const STATIC_FULL_PAGES: Record<string, StaticFullPageEntry> = {
  'webpages:default:legal-notice:WebHome': {
    pageTitle: 'Les mentions légales et les CGU de Nudger',
    metaTitle: 'Mentions légales | Nudger',
    metaDescription: 'Les mentions légales de Nudger',
    width: 'container-semi-fluid',
    html: {
      fr: `<h2>1. Présentation du site et de l’éditeur</h2>
<p><strong>Nom du site :</strong> <a href="https://nudger.fr">https://nudger.fr</a><br>
<strong>Activité :</strong> Comparateur écologique (B2C), en cours d’immatriculation.</p>
<p><strong>Éditeur (ci-après « Nudger ») :</strong><br>
Goulven FURET<br>
Adresse : 8 Venelle de Keralloche, 29200 Brest, France<br>
Statut : Activité en cours d’immatriculation (micro-entrepreneur / SAS à venir)<br>
Responsable de la publication : Goulven FURET (contact : goulven.furet(at)gmail.com)</p>
<p><strong>Hébergement :</strong><br>
Hetzner Online GmbH,<br>
Stuttgarter St. 1<br>
91710 Gunzenhausen – Allemagne</p>
<h2>2. Objet et acceptation des présentes conditions</h2>
<p>Le présent document a pour objet de définir les conditions générales d’utilisation (CGU) du site <a href="https://nudger.fr">https://nudger.fr</a> (ci-après « le Site »). Toute connexion et navigation sur le Site implique l’acceptation pleine et entière des présentes CGU.</p>
<p>L’éditeur (Nudger) se réserve le droit de modifier à tout moment ces CGU. Les utilisateurs sont invités à les consulter régulièrement.</p>
<h2>3. Services fournis - Comparateur écologique</h2>
<p>Nudger propose un comparateur de produits permettant :</p>
<ul>
<li>La comparaison des prix auprès de divers marchands (B2C).</li>
<li>L’affichage d’un <strong>Impact Score</strong> ou « éco-score » indicatif, concernant l’empreinte environnementale potentielle d’un produit.</li>
</ul>
<h3>Avertissement sur l’Impact Score</h3>
<p>L’Impact Score fourni par Nudger est purement indicatif. Il peut comporter des imprécisions ou des erreurs, liées à la qualité ou à la disponibilité des données (ex. informations partielles sur la chaîne de production, codes-barres, etc.). L’utilisateur reconnaît que l’Impact Score ne peut se substituer à une analyse complète du produit, et que Nudger n’est pas responsable de l’exactitude ou de l’exhaustivité de ces informations.</p>
<h3>Gratuité du service</h3>
<p>Le Site est entièrement gratuit pour l’utilisateur. Nudger ne vend pas de produits directement et n’est pas responsable des éventuelles variations de prix pratiquées par des marchands tiers.</p>
<h2>4. Contenu UGC et commentaires</h2>
<p>Les utilisateurs ont la possibilité de poster des commentaires ou du contenu (UGC). En contrepartie, Nudger se réserve le droit de modérer ou de supprimer tout contenu sans avertissement préalable, notamment en cas de contenu illicite, diffamatoire, injurieux, ou contraire aux présentes CGU.</p>
<h2>5. Limitation de responsabilité</h2>
<ol>
<li><strong>Disponibilité des produits et prix</strong><br>
Nudger agit en tant que simple comparateur. Les offres, tarifs et disponibilités sont susceptibles d’être modifiés par les marchands sans préavis. Nudger ne saurait être tenu responsable si le prix ou la disponibilité d’un produit diffère de ce qui est indiqué sur le Site.</li>
<li><strong>Dommages matériels ou immatériels</strong><br>
L’utilisateur s’engage à accéder au Site via un équipement récent et sécurisé. Nudger ne pourra être tenu responsable des dommages directs ou indirects (perte de marché, perte de profit…) résultant de l’utilisation ou de l’impossibilité d’utiliser le Site.</li>
<li><strong>Interruption pour maintenance</strong><br>
Nudger peut être amené à interrompre l’accès au Site pour effectuer des opérations de maintenance. Il s’efforcera de communiquer au préalable les dates et heures d’intervention, sans toutefois que cette obligation soit de résultat.</li>
</ol>
<h2>6. Propriété intellectuelle</h2>
<h3>6.1 Code source et données ouvertes (Open Source / Open Data)</h3>
<ul>
<li>Le code source du projet est disponible sous licence <strong>GNU Affero General Public License</strong> (avec exceptions concernant la vente). Voir le dépôt GitHub : <a href="https://github.com/open4good/open4goods">https://github.com/open4good/open4goods</a>.</li>
<li>Certaines données, notamment les extractions de codes-barres ISBN et GTIN, sont proposées en <strong>Open Data</strong> sous licence <strong>ODbL</strong> (Open Database License). Elles sont consultables à l’adresse <a href="https://nudger.fr/opendata">https://nudger.fr/opendata</a>.</li>
</ul>
<h3>6.2 Éléments graphiques et contenus textuels</h3>
<ul>
<li>Les logos, chartes graphiques, éléments visuels de Nudger demeurent la propriété exclusive de l’Éditeur.</li>
<li>Les textes et contenus éditoriaux présents sur le Site ne sont <strong>pas</strong> réutilisables ni modifiables, sans autorisation écrite préalable de l’Éditeur.</li>
</ul>
<p>Toute reproduction, représentation, adaptation ou exploitation non autorisée engage la responsabilité de son auteur et constitue une contrefaçon réprimée par les articles L.335-2 et suivants du Code de la Propriété Intellectuelle.</p>
<h2>7. Données personnelles</h2>
<p>Nudger traite les données à caractère personnel conformément au Règlement Général sur la Protection des Données (RGPD) n°2016/679. Les informations détaillées sur la collecte et le traitement des données, ainsi que sur vos droits (accès, rectification, suppression, etc.), figurent dans notre <a href="https://nudger.fr/politique-confidentialite">Politique de Confidentialité</a>.</p>
<h2>8. Droit applicable et juridiction</h2>
<p>Les présentes Mentions Légales et CGU sont soumises au droit français. En cas de litige, et sauf dispositions légales impératives contraires, les tribunaux compétents de BREST (France) sont expressément désignés.</p>
<h2>9. Liens utiles</h2>
<ul>
<li><a href="https://nudger.fr/contact">Contact</a></li>
<li><a href="https://nudger.fr/politique-confidentialite">Politique de Confidentialité</a></li>
<li><a href="https://nudger.fr/compensation-ecologique">Contribution écologique</a></li>
</ul>
<hr>
<p><em>« Dernière mise à jour : 6 Janvier 2025 »</em></p>`,
    },
  },
  'webpages:default:data-privacy:WebHome': {
    pageTitle: 'Politique de confidentialité',
    metaTitle: 'Politique de confidentialité | Nudger',
    metaDescription: 'La Politique de confidentialité de Nudger',
    width: 'container-fluid',
    html: {
      fr: `<h2>1. Introduction</h2>
<p>La présente Politique de Confidentialité décrit les modalités de collecte, d’utilisation et de protection des données personnelles lors de votre navigation sur le site <a href="https://nudger.fr">https://nudger.fr</a> (ci-après « le Site »). Nous respectons le Règlement Général sur la Protection des Données (RGPD) (UE 2016/679) et la loi Informatique et Libertés.</p>
<h2>2. Identité du responsable de traitement</h2>
<p><strong>Responsable du traitement :</strong><br>
<a href="https://www.linkedin.com/in/goulven-furet-b448b582/">Goulven FURET</a><br>
8 Venelle de Keralloche, 29200 Brest, France<br>
« Activité en cours d’immatriculation »</p>
<p><strong>Délégué à la Protection des Données (DPO) :</strong><br>
Goulven FURET (contact identique)</p>
<h2>3. Données collectées et finalités</h2>
<h3>3.1 Données collectées automatiquement</h3>
<ul>
<li>
<strong>Adresse IP</strong> et <strong>User Agent</strong> : collectés à des fins de sécurité (prévenir la fraude, garantir la maintenance du Site) et pour établir les compteurs servant à calculer le don aux associations.
<ul><li><strong>Base légale</strong> : intérêt légitime (sécuriser le Site, produire des statistiques d’accès).</li></ul>
</li>
</ul>
<h3>3.2 Formulaire de contact et alertes prix (à venir)</h3>
<ul>
<li>
<strong>Adresse e-mail</strong> (lorsque vous remplissez le formulaire de contact ou que vous demandez une alerte prix).
<ul><li><strong>Base légale</strong> : intérêt légitime (répondre aux demandes) ou consentement explicite (lors de l’inscription aux alertes).</li></ul>
</li>
</ul>
<h3>3.3 Absence de cookies</h3>
<p>Nudger n’utilise <strong>aucun</strong> cookie pour vous suivre ou tracer vos comportements. Nous utilisons l’outil de mesure d’audience <strong>Plausible</strong>, qui est sans cookie et conforme au RGPD.</p>
<h2>4. Durée de conservation</h2>
<ul>
<li><strong>Adresses IP</strong> et logs de navigation : conservés pour une durée maximale de <strong>1 an</strong>, puis supprimés.</li>
<li><strong>Adresses e-mail</strong> (via formulaire de contact ou alertes prix) : conservées le temps nécessaire au traitement de la demande ou à l’envoi des alertes, puis supprimées ou anonymisées si inactives.</li>
</ul>
<h2>5. Destinataires et sous-traitants</h2>
<p>Nous ne communiquons aucune donnée personnelle à des tiers, sauf obligation légale.<br>
Nous n’avons pas recours à des sous-traitants ou prestataires extérieurs pour le moment, à l’exception de :</p>
<ul>
<li><strong>Hébergeur</strong> : Hetzner (dans l’Union Européenne).</li>
</ul>
<h2>6. Vos droits</h2>
<p>En vertu du RGPD, vous disposez des droits suivants :</p>
<ul>
<li><strong>Droit d’accès</strong> : obtenir une copie de vos données personnelles.</li>
<li><strong>Droit de rectification</strong> : corriger les données inexactes ou incomplètes.</li>
<li><strong>Droit d’effacement</strong> : demander la suppression de vos données (sauf obligation légale de conservation).</li>
<li><strong>Droit d’opposition</strong> : vous opposer à certains traitements fondés sur l’intérêt légitime.</li>
<li><strong>Droit à la limitation</strong> : dans certains cas, limiter le traitement de vos données.</li>
<li><strong>Droit à la portabilité</strong> : récupérer les données que vous nous avez fournies, dans un format structuré.</li>
</ul>
<p>Pour exercer vos droits, veuillez nous contacter à l’adresse postale ci-dessus ou par le <a href="https://nudger.fr/contact">formulaire de contact</a>.<br>
Pensez à joindre un justificatif d’identité en cas de demande sensible.</p>
<h2>7. Sécurité des données</h2>
<p>Nous mettons en œuvre des mesures techniques et organisationnelles pour protéger vos données contre la perte, la divulgation ou l’accès non autorisé (chiffrement, pare-feu, etc.). Toutefois, aucun système n’est infaillible et nous ne pouvons garantir une sécurité absolue.</p>
<h2>8. Transfert hors de l’Union Européenne</h2>
<p>Aucun transfert de données hors de l’UE n’est effectué. Nos serveurs sont situés en Allemagne (UE), chez Hetzner.</p>
<h2>9. Évolution de la Politique de Confidentialité</h2>
<p>Nous nous réservons le droit de modifier la présente Politique de Confidentialité à tout moment, pour nous adapter aux évolutions légales ou techniques. Toute nouvelle version sera publiée sur cette même page.</p>
<h2>10. Contact et réclamations</h2>
<p>En cas de question ou de réclamation concernant vos données, vous pouvez nous écrire :<br>
<strong>Adresse :</strong><br>
Nudger – Goulven FURET</p>
<p>Si vous estimez que vos droits ne sont pas respectés, vous pouvez introduire une réclamation auprès de l’autorité de contrôle compétente (CNIL en France : <a href="https://www.cnil.fr/fr/plaintes">https://www.cnil.fr/fr/plaintes</a>).</p>
<hr>
<p><em>« Dernière mise à jour : 6 Janvier 2025 »</em></p>`,
    },
  },
}

/**
 * Looks up a real static full page for a page id, mirroring `getStaticContentBloc`. Falls back
 * from the requested language to French, then returns `null` when the page id has no static
 * entry -- callers should fall through to the live XWiki fetch in that case.
 */
export function getStaticFullPage(
  pageId: string,
  domainLanguage: DomainLanguage
): CmsFullPage | null {
  const entry = STATIC_FULL_PAGES[pageId]
  if (!entry) {
    return null
  }
  const htmlContent = entry.html[domainLanguage] ?? entry.html.fr
  if (!htmlContent) {
    return null
  }
  return {
    htmlContent,
    pageTitle: entry.pageTitle,
    metaTitle: entry.metaTitle,
    metaDescription: entry.metaDescription,
    width: entry.width,
    editLink: null,
  }
}
