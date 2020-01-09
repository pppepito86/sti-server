import React from 'react'
import Header from '../components/Header';
import Sidebar from '../components/Sidebar';
import Footer from '../components/Footer';
import TaskContent from '../components/TaskContent';
import SubmissionContent from '../components/SubmissionContent';
import PdfContent from '../components/PdfContent';
import ScoreboardContent from '../components/ScoreboardContent';
import DashboardContent from '../components/DashboardContent';
import QuestionsContent from '../components/QuestionsContent';
import { AppProvider } from '../AppContext';
import Modal from '../components/Modal';
import AdminDashboardContent from '../components/admin/AdminDashboardContent';
import AdminHeader from '../components/admin/AdminHeader';
import AdminSidebar from '../components/admin/AdminSidebar';

const AdminPageTemplate = ({ content }) => {
    return (
        <AppProvider>
            <div className="wrapper">
                <AdminHeader />
                <AdminSidebar />
                {content === 'task' && <TaskContent />}
                {content === 'submission' && <SubmissionContent />}
                {content === 'pdf' && <PdfContent />}
                {content === 'questions' && <QuestionsContent />}
                {content === 'scoreboard' && <ScoreboardContent />}
                {content === 'dashboard' && <DashboardContent />}
                {content === 'admin' && <AdminDashboardContent />}
                <Footer />
                <Modal />
            </div>
        </AppProvider>
    )
}

export default AdminPageTemplate
