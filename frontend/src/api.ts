import type {Plan,ScanResult,ScanSummary,UserSession} from './types'

const SESSION_KEY='escudo.session'
export const getSession=():UserSession|null=>{try{return JSON.parse(localStorage.getItem(SESSION_KEY)||'null')}catch{return null}}
export const setSession=(value:UserSession|null)=>value?localStorage.setItem(SESSION_KEY,JSON.stringify(value)):localStorage.removeItem(SESSION_KEY)

export class ApiFailure extends Error{constructor(public code:string,message:string){super(message)}}

async function call<T>(path:string,options:RequestInit={}):Promise<T>{
  const session=getSession();const headers=new Headers(options.headers)
  if(!(options.body instanceof FormData))headers.set('Content-Type','application/json')
  if(session)headers.set('Authorization',`Bearer ${session.accessToken}`)
  const response=await fetch(path,{...options,headers})
  if(response.status===204)return undefined as T
  const body=await response.json().catch(()=>({}))
  if(!response.ok)throw new ApiFailure(body.code||'REQUEST_FAILED',body.message||'No pudimos completar la operación.')
  return body as T
}

export const api={
  register:(email:string,password:string)=>call<UserSession>('/api/auth/register',{method:'POST',body:JSON.stringify({email,password})}),
  login:(email:string,password:string)=>call<UserSession>('/api/auth/login',{method:'POST',body:JSON.stringify({email,password})}),
  scan:(type:'TEXT'|'URL',content:string)=>call<ScanResult>(`/api/scans/${type.toLowerCase()}`,{method:'POST',body:JSON.stringify({content})}),
  scanImage:(file:File,context:string)=>{const data=new FormData();data.append('file',file);if(context)data.append('context',context);return call<ScanResult>('/api/scans/image',{method:'POST',body:data})},
  history:()=>call<{content:ScanSummary[]}>('/api/scans'),
  detail:(id:string)=>call<ScanResult>(`/api/scans/${id}`),
  plans:()=>call<Plan[]>('/api/plans'),
  scanCheckout:()=>call<{checkoutUrl:string}>('/api/payments/scan-checkout',{method:'POST'}),
  subscribe:(planCode:string)=>call<{checkoutUrl:string}>('/api/subscription/checkout',{method:'POST',body:JSON.stringify({planCode})}),
  adminUsers:()=>call<{content:Array<{id:string;email:string;status:string;roles:string[]}>}>('/api/admin/users'),
  riskConfig:()=>call<Array<{component:string;weight:number;version:string}>>('/api/admin/risk-config'),
  saveRiskConfig:(rows:Array<{component:string;weight:number}>)=>call('/api/admin/risk-config',{method:'PUT',body:JSON.stringify(rows)})
}

