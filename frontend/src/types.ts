export type Indicator={type:string;category:string;score:number;severity:string;source:string;confidence:number;explanation:string}
export type ScanResult={id:string;type:string;status:string;score:number;level:string;classification:string;confidence:number;engineVersion:string;summary:string;recommendedAction:string;indicators:Indicator[];createdAt:string;guest:boolean}
export type ScanSummary={id:string;type:string;score:number;level:string;classification:string;createdAt:string}
export type UserSession={accessToken:string;refreshToken:string;userId:string;email:string;roles:string[]}
export type Plan={code:string;name:string;amount:number;currency:string;limitsJson:string;featuresJson:string}

