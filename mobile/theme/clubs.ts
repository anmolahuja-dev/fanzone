/**
 * Club-specific theme definitions.
 * Each club has unique colors that permeate the entire app experience.
 */

export interface ClubTheme {
  id: string;
  name: string;
  shortName: string;
  colors: {
    primary: string;
    secondary: string;
    primaryGlow: string;
    gradient: [string, string];
  };
  crestUrl: string;
}

export const CLUB_THEMES: Record<string, ClubTheme> = {
  // Premier League
  liverpool: {
    id: 'liverpool',
    name: 'Liverpool FC',
    shortName: 'LIV',
    colors: {
      primary: '#C8102E',
      secondary: '#F6EB61',
      primaryGlow: 'rgba(200,16,46,0.15)',
      gradient: ['#C8102E', '#8B0000'],
    },
    crestUrl: '/clubs/liverpool.png',
  },
  manchester_city: {
    id: 'manchester_city',
    name: 'Manchester City',
    shortName: 'MCI',
    colors: {
      primary: '#6CABDD',
      secondary: '#1C2C5B',
      primaryGlow: 'rgba(108,171,221,0.15)',
      gradient: ['#6CABDD', '#1C2C5B'],
    },
    crestUrl: '/clubs/man-city.png',
  },
  arsenal: {
    id: 'arsenal',
    name: 'Arsenal',
    shortName: 'ARS',
    colors: {
      primary: '#EF0107',
      secondary: '#063672',
      primaryGlow: 'rgba(239,1,7,0.15)',
      gradient: ['#EF0107', '#9C1519'],
    },
    crestUrl: '/clubs/arsenal.png',
  },
  chelsea: {
    id: 'chelsea',
    name: 'Chelsea FC',
    shortName: 'CHE',
    colors: {
      primary: '#034694',
      secondary: '#DBA111',
      primaryGlow: 'rgba(3,70,148,0.15)',
      gradient: ['#034694', '#001C3D'],
    },
    crestUrl: '/clubs/chelsea.png',
  },
  manchester_united: {
    id: 'manchester_united',
    name: 'Manchester United',
    shortName: 'MUN',
    colors: {
      primary: '#DA291C',
      secondary: '#FBE122',
      primaryGlow: 'rgba(218,41,28,0.15)',
      gradient: ['#DA291C', '#8B0000'],
    },
    crestUrl: '/clubs/man-utd.png',
  },
  tottenham: {
    id: 'tottenham',
    name: 'Tottenham Hotspur',
    shortName: 'TOT',
    colors: {
      primary: '#132257',
      secondary: '#FFFFFF',
      primaryGlow: 'rgba(19,34,87,0.15)',
      gradient: ['#132257', '#0A1128'],
    },
    crestUrl: '/clubs/tottenham.png',
  },
  // La Liga
  barcelona: {
    id: 'barcelona',
    name: 'FC Barcelona',
    shortName: 'BAR',
    colors: {
      primary: '#004D98',
      secondary: '#A50044',
      primaryGlow: 'rgba(0,77,152,0.15)',
      gradient: ['#004D98', '#A50044'],
    },
    crestUrl: '/clubs/barcelona.png',
  },
  real_madrid: {
    id: 'real_madrid',
    name: 'Real Madrid',
    shortName: 'RMA',
    colors: {
      primary: '#FEBE10',
      secondary: '#00529F',
      primaryGlow: 'rgba(254,190,16,0.12)',
      gradient: ['#FEBE10', '#D4A017'],
    },
    crestUrl: '/clubs/real-madrid.png',
  },
  atletico_madrid: {
    id: 'atletico_madrid',
    name: 'Atletico Madrid',
    shortName: 'ATM',
    colors: {
      primary: '#CB3524',
      secondary: '#272E61',
      primaryGlow: 'rgba(203,53,36,0.15)',
      gradient: ['#CB3524', '#272E61'],
    },
    crestUrl: '/clubs/atletico.png',
  },
  // Bundesliga
  bayern_munich: {
    id: 'bayern_munich',
    name: 'Bayern Munich',
    shortName: 'BAY',
    colors: {
      primary: '#DC052D',
      secondary: '#0066B2',
      primaryGlow: 'rgba(220,5,45,0.15)',
      gradient: ['#DC052D', '#8B0000'],
    },
    crestUrl: '/clubs/bayern.png',
  },
  borussia_dortmund: {
    id: 'borussia_dortmund',
    name: 'Borussia Dortmund',
    shortName: 'BVB',
    colors: {
      primary: '#FDE100',
      secondary: '#000000',
      primaryGlow: 'rgba(253,225,0,0.12)',
      gradient: ['#FDE100', '#C8B400'],
    },
    crestUrl: '/clubs/dortmund.png',
  },
  // Serie A
  ac_milan: {
    id: 'ac_milan',
    name: 'AC Milan',
    shortName: 'MIL',
    colors: {
      primary: '#FB090B',
      secondary: '#000000',
      primaryGlow: 'rgba(251,9,11,0.15)',
      gradient: ['#FB090B', '#8B0000'],
    },
    crestUrl: '/clubs/ac-milan.png',
  },
  inter_milan: {
    id: 'inter_milan',
    name: 'Inter Milan',
    shortName: 'INT',
    colors: {
      primary: '#010E80',
      secondary: '#F5A503',
      primaryGlow: 'rgba(1,14,128,0.15)',
      gradient: ['#010E80', '#000033'],
    },
    crestUrl: '/clubs/inter.png',
  },
  juventus: {
    id: 'juventus',
    name: 'Juventus',
    shortName: 'JUV',
    colors: {
      primary: '#000000',
      secondary: '#DDB771',
      primaryGlow: 'rgba(221,183,113,0.12)',
      gradient: ['#000000', '#1A1A1A'],
    },
    crestUrl: '/clubs/juventus.png',
  },
  // Ligue 1
  psg: {
    id: 'psg',
    name: 'Paris Saint-Germain',
    shortName: 'PSG',
    colors: {
      primary: '#004170',
      secondary: '#DA291C',
      primaryGlow: 'rgba(0,65,112,0.15)',
      gradient: ['#004170', '#001C3D'],
    },
    crestUrl: '/clubs/psg.png',
  },
  // More clubs
  newcastle: {
    id: 'newcastle',
    name: 'Newcastle United',
    shortName: 'NEW',
    colors: {
      primary: '#241F20',
      secondary: '#F1BE48',
      primaryGlow: 'rgba(241,190,72,0.12)',
      gradient: ['#241F20', '#000000'],
    },
    crestUrl: '/clubs/newcastle.png',
  },
  aston_villa: {
    id: 'aston_villa',
    name: 'Aston Villa',
    shortName: 'AVL',
    colors: {
      primary: '#670E36',
      secondary: '#95BFE5',
      primaryGlow: 'rgba(103,14,54,0.15)',
      gradient: ['#670E36', '#3D0820'],
    },
    crestUrl: '/clubs/aston-villa.png',
  },
  west_ham: {
    id: 'west_ham',
    name: 'West Ham United',
    shortName: 'WHU',
    colors: {
      primary: '#7A263A',
      secondary: '#1BB1E7',
      primaryGlow: 'rgba(122,38,58,0.15)',
      gradient: ['#7A263A', '#4A1623'],
    },
    crestUrl: '/clubs/west-ham.png',
  },
  brighton: {
    id: 'brighton',
    name: 'Brighton & Hove Albion',
    shortName: 'BHA',
    colors: {
      primary: '#0057B8',
      secondary: '#FFCD00',
      primaryGlow: 'rgba(0,87,184,0.15)',
      gradient: ['#0057B8', '#003D82'],
    },
    crestUrl: '/clubs/brighton.png',
  },
  napoli: {
    id: 'napoli',
    name: 'SSC Napoli',
    shortName: 'NAP',
    colors: {
      primary: '#12A0D7',
      secondary: '#FFFFFF',
      primaryGlow: 'rgba(18,160,215,0.15)',
      gradient: ['#12A0D7', '#0A6E94'],
    },
    crestUrl: '/clubs/napoli.png',
  },
};

export const DEFAULT_CLUB_THEME: ClubTheme = {
  id: 'default',
  name: 'Fanzone',
  shortName: 'FZ',
  colors: {
    primary: '#6C63FF',
    secondary: '#FF6584',
    primaryGlow: 'rgba(108,99,255,0.15)',
    gradient: ['#6C63FF', '#4A42E0'],
  },
  crestUrl: '/clubs/fanzone.png',
};
